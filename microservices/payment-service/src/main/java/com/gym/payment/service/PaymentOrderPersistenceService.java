
package com.gym.payment.service;

import com.gym.payment.entity.Payment;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.exception.PaymentNotFoundException;
import com.gym.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class PaymentOrderPersistenceService {

    private final PaymentRepository paymentRepository;

    public PaymentOrderPersistenceService(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPayment(
            Long memberId,
            String planName,
            long amountPaise,
            String idempotencyKey
    ) {
        Payment payment = Payment.createNew();

        payment.setMemberId(memberId);
        payment.setPlanName(planName);
        payment.setAmountPaise(amountPaise);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setIdempotencyKey(idempotencyKey);

        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markOrderCreated(
            Long paymentId,
            String razorpayOrderId
    ) {
        Payment payment = findPayment(paymentId);

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID is required"
            );
        }

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setFailureReason(null);

        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderFailed(
            Long paymentId,
            String reason
    ) {
        Payment payment = findPayment(paymentId);

        // Only a payment without a Razorpay order can be marked failed here.
        if (payment.getStatus() != PaymentStatus.CREATED
                || payment.getRazorpayOrderId() != null) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(safeReason(reason));

        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderOutcomeUnknown(Long paymentId, String reason) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.CREATED
                || payment.getRazorpayOrderId() != null) {
            return;
        }

        payment.setFailureReason(safeReason(reason));
        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markPaymentSuccessful(
            Long memberId,
            String orderId,
            RazorpayService.RazorpayPayment gatewayPayment,
            Long membershipId
    ) {
        return markPaymentSuccessful(memberId, orderId, gatewayPayment, membershipId, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markPaymentSuccessful(
            Long memberId,
            String orderId,
            RazorpayService.RazorpayPayment gatewayPayment,
            Long membershipId,
            LocalDate membershipStartDate,
            LocalDate membershipEndDate
    ) {
        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment order was not found"
                ));

        if (!payment.getMemberId().equals(memberId)) {
            throw new PaymentNotFoundException("Payment order was not found");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (gatewayPayment.paymentId().equals(payment.getRazorpayPaymentId())) {
                return payment;
            }
            throw new IllegalStateException(
                    "Payment order has already been completed"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is no longer awaiting verification"
            );
        }

        if (!orderId.equals(gatewayPayment.orderId())
                || !payment.getAmountPaise().equals(gatewayPayment.amountPaise())
                || !payment.getCurrency().equals(gatewayPayment.currency())
                || !"captured".equalsIgnoreCase(gatewayPayment.status())) {
            throw new IllegalArgumentException(
                    "Razorpay payment details do not match the stored order"
            );
        }

        payment.setRazorpayPaymentId(gatewayPayment.paymentId());
        payment.setMembershipId(membershipId);
        payment.setMembershipStartDate(membershipStartDate);
        payment.setMembershipEndDate(membershipEndDate);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);
        payment.setVerifiedAt(Instant.now());
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment claimReceiptEmail(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment record was not found"));
        if (payment.getStatus() != PaymentStatus.SUCCESS || payment.getReceiptEmailSentAt() != null) return null;
        Instant now = Instant.now();
        Instant lastAttempt = payment.getReceiptEmailClaimedAt();
        if (lastAttempt != null && lastAttempt.isAfter(now.minus(Duration.ofMinutes(5)))) return null;
        payment.setReceiptEmailClaimedAt(now);
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReceiptEmailSent(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS) return;
        payment.setReceiptEmailSentAt(Instant.now());
        payment.setReceiptEmailClaimedAt(null);
        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkMembership(Long paymentId, Long membershipId) {
        linkMembership(paymentId, membershipId, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkMembership(Long paymentId, Long membershipId, LocalDate startDate, LocalDate endDate) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only a verified payment can be linked to a membership");
        }
        if (payment.getMembershipId() != null && !payment.getMembershipId().equals(membershipId)) {
            throw new IllegalStateException("Payment is already linked to another membership");
        }
        payment.setMembershipId(membershipId);
        payment.setMembershipStartDate(startDate);
        payment.setMembershipEndDate(endDate);
        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(readOnly = true)
    public Payment findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment record was not found"
                ));
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Payment order creation failed";
        }

        // Payment.failure_reason column has length 500.
        return reason.length() > 500
                ? reason.substring(0, 500)
                : reason;
    }
}
