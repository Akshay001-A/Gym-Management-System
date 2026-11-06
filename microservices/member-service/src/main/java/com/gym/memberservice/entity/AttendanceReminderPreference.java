package com.gym.memberservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "member_attendance_reminder_preferences")
public class AttendanceReminderPreference {
    @Id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_reminder_sent_for")
    private LocalDate lastReminderSentFor;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceReminderPreference() {}

    public AttendanceReminderPreference(Long memberId, boolean enabled) {
        this.memberId = memberId;
        this.enabled = enabled;
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getMemberId() { return memberId; }
    public boolean isEnabled() { return enabled; }
    public LocalDate getLastReminderSentFor() { return lastReminderSentFor; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLastReminderSentFor(LocalDate date) { this.lastReminderSentFor = date; }
}
