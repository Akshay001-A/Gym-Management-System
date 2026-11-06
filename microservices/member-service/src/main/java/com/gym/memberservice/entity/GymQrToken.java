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
@Table(name = "member_attendance_qr_tokens", indexes = {
        @Index(name = "idx_attendance_qr_expiry", columnList = "expires_at")
})
public class GymQrToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "issued_by", nullable = false, length = 120)
    private String issuedBy;

    protected GymQrToken() {}

    public GymQrToken(String tokenHash, Instant createdAt, Instant expiresAt, String issuedBy) {
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.issuedBy = issuedBy;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getIssuedBy() { return issuedBy; }
}
