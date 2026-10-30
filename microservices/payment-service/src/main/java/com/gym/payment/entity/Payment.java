
package com.gym.payment.entity;

import com.gym.payment.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "payments",
        indexes = @Index(
                name = "ix_payments_member_created_at",
                columnList = "member_id, created_at"
        ),
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_idempotency_key",
                        columnNames = "idempotency_key"
                ),
                @UniqueConstraint(
                        name = "uk_payment_razorpay_order_id",
                        columnNames = "razorpay_order_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_razorpay_payment_id",
                        columnNames = "razorpay_payment_id"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "plan_name", nullable = false, length = 30)
    private String planName;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(name = "razorpay_order_id", unique = true, length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true, length = 100)
    private String razorpayPaymentId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "membership_id")
    private Long membershipId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "membership_start_date")
    private java.time.LocalDate membershipStartDate;

    @Column(name = "membership_end_date")
    private java.time.LocalDate membershipEndDate;

    @Column(name = "receipt_email_claimed_at")
    private Instant receiptEmailClaimedAt;

    @Column(name = "receipt_email_sent_at")
    private Instant receiptEmailSentAt;

    protected Payment() {
    }

    public static Payment createNew() {
        return new Payment();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public java.time.LocalDate getMembershipStartDate() { return membershipStartDate; }

    public void setMembershipStartDate(java.time.LocalDate membershipStartDate) { this.membershipStartDate = membershipStartDate; }

    public java.time.LocalDate getMembershipEndDate() { return membershipEndDate; }

    public void setMembershipEndDate(java.time.LocalDate membershipEndDate) { this.membershipEndDate = membershipEndDate; }

    public Instant getReceiptEmailClaimedAt() { return receiptEmailClaimedAt; }

    public void setReceiptEmailClaimedAt(Instant receiptEmailClaimedAt) { this.receiptEmailClaimedAt = receiptEmailClaimedAt; }

    public Instant getReceiptEmailSentAt() { return receiptEmailSentAt; }

    public void setReceiptEmailSentAt(Instant receiptEmailSentAt) { this.receiptEmailSentAt = receiptEmailSentAt; }
}
