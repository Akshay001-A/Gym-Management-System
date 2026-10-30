
package com.gym.payment.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "razorpay_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_razorpay_webhook_event_id",
                        columnNames = "event_id"
                )
        }
)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus = "RECEIVED";

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    protected WebhookEvent() {
    }

    public WebhookEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.receivedAt = Instant.now();
        this.processingStatus = "RECEIVED";
        this.attemptCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public void recordProcessingAttempt() {
        this.attemptCount++;
        this.lastAttemptAt = Instant.now();
        this.processingStatus = "PROCESSING";
        this.failureReason = null;
    }

    public void markProcessed() {
        this.processingStatus = "PROCESSED";
        this.processedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.processingStatus = "FAILED";
        this.failureReason = reason;
        this.processedAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
