package com.gym.memberservice.service;

import com.gym.memberservice.dto.AttendanceDtos.AttendanceReminderMember;
import com.gym.memberservice.dto.AttendanceDtos.AttendanceReminderSettings;
import com.gym.memberservice.entity.AttendanceReminderPreference;
import com.gym.memberservice.entity.Member;
import com.gym.memberservice.repository.AttendanceReminderPreferenceRepository;
import com.gym.memberservice.repository.AttendanceVisitRepository;
import com.gym.memberservice.repository.MemberRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceReminderService {
    private final AttendanceReminderPreferenceRepository preferenceRepository;
    private final AttendanceVisitRepository visitRepository;
    private final MemberRepository memberRepository;
    private final AttendanceReminderEmailService emailService;
    private final ZoneId timeZone;

    public AttendanceReminderService(AttendanceReminderPreferenceRepository preferenceRepository,
            AttendanceVisitRepository visitRepository, MemberRepository memberRepository,
            AttendanceReminderEmailService emailService,
            @Value("${app.attendance.time-zone:Asia/Kolkata}") String timeZone) {
        this.preferenceRepository = preferenceRepository;
        this.visitRepository = visitRepository;
        this.memberRepository = memberRepository;
        this.emailService = emailService;
        this.timeZone = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public AttendanceReminderSettings getSettings() {
        Map<Long, AttendanceReminderPreference> preferences = preferenceRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceReminderPreference::getMemberId, preference -> preference));
        List<AttendanceReminderMember> members = memberRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream().map(member -> toDto(member, preferences.get(member.getId()))).toList();
        return new AttendanceReminderSettings(emailService.isConfigured(), members);
    }

    @Transactional
    public AttendanceReminderMember setEnabled(Long memberId, boolean enabled) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member was not found"));
        if (enabled && (member.getEmail() == null || member.getEmail().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This member does not have an email address");
        }
        if (enabled && !emailService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email reminders are unavailable because SMTP settings are not configured");
        }

        AttendanceReminderPreference preference = preferenceRepository.findById(memberId)
                .orElseGet(() -> new AttendanceReminderPreference(memberId, false));
        preference.setEnabled(enabled);
        return toDto(member, preferenceRepository.save(preference));
    }

    @Transactional
    public void sendIfAbsent(Long memberId, LocalDate absentDate) throws Exception {
        AttendanceReminderPreference preference = preferenceRepository.findByMemberIdForUpdate(memberId).orElse(null);
        if (preference == null || !preference.isEnabled()
                || absentDate.equals(preference.getLastReminderSentFor())) return;

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null || member.getEmail() == null || member.getEmail().isBlank()) return;

        Instant dayStart = absentDate.atStartOfDay(timeZone).toInstant();
        Instant dayEnd = absentDate.plusDays(1).atStartOfDay(timeZone).toInstant();
        if (!visitRepository.findOverlappingVisits(memberId, dayStart, dayEnd).isEmpty()) return;

        emailService.sendReminder(member.getEmail(), member.getName());
        preference.setLastReminderSentFor(absentDate);
        preferenceRepository.save(preference);
    }

    private AttendanceReminderMember toDto(Member member, AttendanceReminderPreference preference) {
        return new AttendanceReminderMember(member.getId(), member.getName(), member.getEmail(),
                preference != null && preference.isEnabled(),
                preference == null ? null : preference.getLastReminderSentFor());
    }
}
