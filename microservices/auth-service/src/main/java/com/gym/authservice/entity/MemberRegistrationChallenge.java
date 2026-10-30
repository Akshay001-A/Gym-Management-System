package com.gym.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "member_registration_challenges")
public class MemberRegistrationChallenge {
    @Id
    @Column(length = 150)
    private String email;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int attempts;

    protected MemberRegistrationChallenge() { }

    public MemberRegistrationChallenge(String email, Long memberId, String codeHash, Instant expiresAt) {
        this.email = email;
        this.memberId = memberId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.attempts = 0;
    }

    public String getEmail() { return email; }
    public Long getMemberId() { return memberId; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}
