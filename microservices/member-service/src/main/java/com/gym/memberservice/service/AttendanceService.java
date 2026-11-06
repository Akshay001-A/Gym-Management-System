package com.gym.memberservice.service;

import com.gym.memberservice.dto.AttendanceDtos.AdminVisit;
import com.gym.memberservice.dto.AttendanceDtos.DaySummary;
import com.gym.memberservice.dto.AttendanceDtos.MemberHistory;
import com.gym.memberservice.dto.AttendanceDtos.QrTokenResponse;
import com.gym.memberservice.dto.AttendanceDtos.ScanResponse;
import com.gym.memberservice.dto.AttendanceDtos.VisitSummary;
import com.gym.memberservice.entity.AttendanceVisit;
import com.gym.memberservice.entity.GymQrToken;
import com.gym.memberservice.entity.Member;
import com.gym.memberservice.repository.AttendanceVisitRepository;
import com.gym.memberservice.repository.GymQrTokenRepository;
import com.gym.memberservice.repository.MemberRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AttendanceVisitRepository visitRepository;
    private final GymQrTokenRepository qrTokenRepository;
    private final MemberRepository memberRepository;
    private final long qrTokenTtlSeconds;
    private final long scanCooldownSeconds;
    private final ZoneId timeZone;

    public AttendanceService(AttendanceVisitRepository visitRepository,
            GymQrTokenRepository qrTokenRepository, MemberRepository memberRepository,
            @Value("${app.attendance.qr-token-ttl-seconds:90}") long qrTokenTtlSeconds,
            @Value("${app.attendance.scan-cooldown-seconds:5}") long scanCooldownSeconds,
            @Value("${app.attendance.time-zone:Asia/Kolkata}") String timeZone) {
        if (qrTokenTtlSeconds < 15 || qrTokenTtlSeconds > 300) {
            throw new IllegalArgumentException("Attendance QR token TTL must be between 15 and 300 seconds");
        }
        if (scanCooldownSeconds < 1 || scanCooldownSeconds > 30) {
            throw new IllegalArgumentException("Attendance scan cooldown must be between 1 and 30 seconds");
        }
        this.visitRepository = visitRepository;
        this.qrTokenRepository = qrTokenRepository;
        this.memberRepository = memberRepository;
        this.qrTokenTtlSeconds = qrTokenTtlSeconds;
        this.scanCooldownSeconds = scanCooldownSeconds;
        this.timeZone = ZoneId.of(timeZone);
    }

    @Transactional
    public QrTokenResponse issueQrToken(String issuedBy) {
        Instant now = Instant.now();
        qrTokenRepository.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.DAYS));

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String qrPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = now.plusSeconds(qrTokenTtlSeconds);
        qrTokenRepository.save(new GymQrToken(sha256(qrPayload), now, expiresAt,
                issuedBy == null || issuedBy.isBlank() ? "ADMIN" : issuedBy));
        return new QrTokenResponse(qrPayload, qrImageDataUrl(qrPayload), expiresAt);
    }

    @Transactional
    public ScanResponse scan(Long memberId, String qrPayload) {
        Instant now = Instant.now();
        GymQrToken qrToken = qrTokenRepository.findByTokenHash(sha256(qrPayload))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code is invalid or expired"));
        if (!qrToken.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "QR code is invalid or expired");
        }

        Member member = memberRepository.findForAttendanceUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Member profile is not available"));

        AttendanceVisit openVisit = visitRepository
                .findFirstByMemberIdAndCheckOutAtIsNullOrderByCheckInAtDesc(member.getId()).orElse(null);
        AttendanceVisit latestVisit = visitRepository.findFirstByMemberIdOrderByCheckInAtDesc(member.getId()).orElse(null);
        if (latestVisit != null) {
            Instant lastActionAt = latestVisit.getCheckOutAt() == null
                    ? latestVisit.getCheckInAt() : latestVisit.getCheckOutAt();
            if (lastActionAt.plusSeconds(scanCooldownSeconds).isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Please wait a few seconds before scanning again");
            }
        }

        String action;
        AttendanceVisit visit;
        if (openVisit == null) {
            visit = visitRepository.save(new AttendanceVisit(member.getId(), now));
            action = "CHECKED_IN";
        } else {
            openVisit.setCheckOutAt(now);
            visit = visitRepository.save(openVisit);
            action = "CHECKED_OUT";
        }

        long visitSeconds = secondsBetween(visit.getCheckInAt(),
                visit.getCheckOutAt() == null ? now : visit.getCheckOutAt());
        long dailyTotalSeconds = dailyDurationSeconds(member.getId(), LocalDate.now(timeZone), now);
        return new ScanResponse(action, visit.getId(), visit.getCheckInAt(), visit.getCheckOutAt(),
                visitSeconds, dailyTotalSeconds, visit.getCheckOutAt() == null);
    }

    @Transactional(readOnly = true)
    public MemberHistory getMemberHistory(Long memberId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(timeZone);
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);
        validateDateRange(from, to, today);

        Instant rangeStart = from.atStartOfDay(timeZone).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(timeZone).toInstant();
        Instant now = Instant.now();
        List<AttendanceVisit> visits = visitRepository.findOverlappingVisits(memberId, rangeStart, rangeEnd);
        List<DaySummary> days = new ArrayList<>();
        long totalSeconds = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(timeZone).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(timeZone).toInstant();
            long dailySeconds = 0;
            int visitCount = 0;
            for (AttendanceVisit visit : visits) {
                Instant effectiveEnd = visit.getCheckOutAt() == null ? now : visit.getCheckOutAt();
                if (visit.getCheckInAt().isBefore(dayEnd) && effectiveEnd.isAfter(dayStart)) {
                    Instant clippedStart = visit.getCheckInAt().isAfter(dayStart) ? visit.getCheckInAt() : dayStart;
                    Instant clippedEnd = effectiveEnd.isBefore(dayEnd) ? effectiveEnd : dayEnd;
                    dailySeconds += secondsBetween(clippedStart, clippedEnd);
                    visitCount++;
                }
            }
            totalSeconds += dailySeconds;
            days.add(new DaySummary(date, dailySeconds, visitCount));
        }

        AttendanceVisit openVisit = visitRepository
                .findFirstByMemberIdAndCheckOutAtIsNullOrderByCheckInAtDesc(memberId).orElse(null);
        List<VisitSummary> visitSummaries = visits.stream().map(visit -> new VisitSummary(
                visit.getId(), visit.getCheckInAt(), visit.getCheckOutAt(),
                secondsBetween(visit.getCheckInAt(), visit.getCheckOutAt() == null ? now : visit.getCheckOutAt()),
                visit.getCheckOutAt() == null)).toList();
        return new MemberHistory(from, to, openVisit != null,
                openVisit == null ? null : openVisit.getCheckInAt(), totalSeconds, days, visitSummaries);
    }

    @Transactional(readOnly = true)
    public Page<AdminVisit> getAdminAttendance(LocalDate date, int page, int size) {
        LocalDate selectedDate = date == null ? LocalDate.now(timeZone) : date;
        validateDateRange(selectedDate, selectedDate, LocalDate.now(timeZone));
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        Instant rangeStart = selectedDate.atStartOfDay(timeZone).toInstant();
        Instant rangeEnd = selectedDate.plusDays(1).atStartOfDay(timeZone).toInstant();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkInAt"));
        Page<AttendanceVisit> visits = visitRepository.findAllOverlappingVisits(rangeStart, rangeEnd, pageable);
        List<Long> memberIds = visits.getContent().stream().map(AttendanceVisit::getMemberId).distinct().toList();
        Map<Long, String> memberNames = new HashMap<>();
        memberRepository.findAllById(memberIds).forEach(member -> memberNames.put(member.getId(), member.getName()));
        Instant now = Instant.now();
        return visits.map(visit -> new AdminVisit(visit.getId(), visit.getMemberId(),
                memberNames.getOrDefault(visit.getMemberId(), "Member #" + visit.getMemberId()),
                visit.getCheckInAt(), visit.getCheckOutAt(),
                secondsBetween(visit.getCheckInAt(), visit.getCheckOutAt() == null ? now : visit.getCheckOutAt()),
                visit.getCheckOutAt() == null));
    }

    private long dailyDurationSeconds(Long memberId, LocalDate date, Instant now) {
        Instant dayStart = date.atStartOfDay(timeZone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(timeZone).toInstant();
        return visitRepository.findOverlappingVisits(memberId, dayStart, dayEnd).stream()
                .mapToLong(visit -> {
                    Instant effectiveEnd = visit.getCheckOutAt() == null ? now : visit.getCheckOutAt();
                    Instant clippedStart = visit.getCheckInAt().isAfter(dayStart) ? visit.getCheckInAt() : dayStart;
                    Instant clippedEnd = effectiveEnd.isBefore(dayEnd) ? effectiveEnd : dayEnd;
                    return secondsBetween(clippedStart, clippedEnd);
                }).sum();
    }

    private void validateDateRange(LocalDate from, LocalDate to, LocalDate today) {
        if (from.isAfter(to) || to.isAfter(today) || ChronoUnit.DAYS.between(from, to) > 89) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range must be ordered, not in the future, and no longer than 90 days");
        }
    }

    private long secondsBetween(Instant start, Instant end) {
        return Math.max(0, Duration.between(start, end).getSeconds());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormatHolder.toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String qrImageDataUrl(String payload) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF173B2D : 0xFFFFFFFF);
                }
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!javax.imageio.ImageIO.write(image, "PNG", output)) {
                    throw new IllegalStateException("PNG image encoding is unavailable");
                }
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            }
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Could not generate attendance QR image", exception);
        }
    }

    private static final class HexFormatHolder {
        private static String toHex(byte[] bytes) {
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        }
    }
}
