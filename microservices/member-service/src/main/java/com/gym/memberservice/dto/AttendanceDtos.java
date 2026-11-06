package com.gym.memberservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record QrTokenResponse(String qrPayload, String qrImageDataUrl, Instant expiresAt) {}

    public record ScanRequest(
            @NotBlank(message = "QR payload is required")
            @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "QR payload is invalid")
            String qrPayload) {}

    public record ScanResponse(String action, Long visitId, Instant checkInAt, Instant checkOutAt,
            long visitDurationSeconds, long dailyTotalSeconds, boolean currentlyInside) {}

    public record VisitSummary(Long visitId, Instant checkInAt, Instant checkOutAt,
            long durationSeconds, boolean currentlyInside) {}

    public record DaySummary(LocalDate date, long totalDurationSeconds, int visitCount) {}

    public record MemberHistory(LocalDate from, LocalDate to, boolean currentlyInside,
            Instant currentCheckInAt, long totalDurationSeconds, List<DaySummary> days,
            List<VisitSummary> visits) {}

    public record AdminVisit(Long visitId, Long memberId, String memberName,
            Instant checkInAt, Instant checkOutAt, long durationSeconds, boolean currentlyInside) {}
}
