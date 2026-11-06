package com.gym.memberservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "member_attendance_visits", indexes = {
        @Index(name = "idx_attendance_member_checkin", columnList = "member_id,check_in_at"),
        @Index(name = "idx_attendance_open_visit", columnList = "member_id,check_out_at")
})
public class AttendanceVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    protected AttendanceVisit() {}

    public AttendanceVisit(Long memberId, Instant checkInAt) {
        this.memberId = memberId;
        this.checkInAt = checkInAt;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Instant getCheckInAt() { return checkInAt; }
    public Instant getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(Instant checkOutAt) { this.checkOutAt = checkOutAt; }
}
