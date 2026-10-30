package com.gym.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_change_challenges")
public class EmailChangeChallenge {
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "new_email", nullable = false, length = 150)
    private String newEmail;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int attempts;

    protected EmailChangeChallenge() { }

    public EmailChangeChallenge(Long accountId, String newEmail, String codeHash, Instant expiresAt) {
        this.accountId = accountId;
        this.newEmail = newEmail;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.attempts = 0;
    }

    public Long getAccountId() { return accountId; }
    public String getNewEmail() { return newEmail; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}
