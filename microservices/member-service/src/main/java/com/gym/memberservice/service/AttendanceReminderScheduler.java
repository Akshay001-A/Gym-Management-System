package com.gym.memberservice.service;

import com.gym.memberservice.repository.AttendanceReminderPreferenceRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(AttendanceReminderScheduler.class);

    private final AttendanceReminderPreferenceRepository preferenceRepository;
    private final AttendanceReminderService reminderService;
    private final AttendanceReminderEmailService emailService;
    private final ZoneId timeZone;

    public AttendanceReminderScheduler(AttendanceReminderPreferenceRepository preferenceRepository,
            AttendanceReminderService reminderService, AttendanceReminderEmailService emailService,
            @Value("${app.attendance.time-zone:Asia/Kolkata}") String timeZone) {
        this.preferenceRepository = preferenceRepository;
        this.reminderService = reminderService;
        this.emailService = emailService;
        this.timeZone = ZoneId.of(timeZone);
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "${app.attendance.time-zone:Asia/Kolkata}")
    public void sendYesterdayAbsenceReminders() {
        if (!emailService.isConfigured()) {
            log.warn("Attendance reminder emails skipped because SMTP is not configured");
            return;
        }

        LocalDate yesterday = LocalDate.now(timeZone).minusDays(1);
        preferenceRepository.findAllByEnabledTrue().forEach(preference -> {
            try {
                reminderService.sendIfAbsent(preference.getMemberId(), yesterday);
            } catch (Exception exception) {
                log.warn("Could not send attendance reminder; memberId={}, errorType={}",
                        preference.getMemberId(), exception.getClass().getSimpleName());
            }
        });
    }
}
