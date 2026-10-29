
package com.gym.payment.service;

import com.gym.payment.dto.CreatePaymentResponse;
import com.gym.payment.dto.PaymentResponse;
import com.gym.payment.dto.VerifyPaymentRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final MembershipIntegrationService membershipIntegrationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            RazorpayService razorpayService,
            MembershipIntegrationService membershipIntegrationService
    ) {
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.membershipIntegrationService = membershipIntegrationService;
    }

    @Transactional
    public CreatePaymentResponse createPaymentOrder(
            Long memberId,
            String planName,
            String idempotencyKey
    ) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Invalid member ID");
        }

        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "A valid idempotency key is required"
            );
        }

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            Payment payment = existing.get();

            if (!payment.getMemberId().equals(memberId)
                    || !payment.getPlanName().equals(planName)) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for another request"
                );
            }

            if (payment.getRazorpayOrderId() != null) {
                return toCreatePaymentResponse(payment);
            }

            throw new IllegalStateException(
                    "A payment request with this key is already being processed"
            );
        }

        // The price must come from the trusted Membership Service catalogue.
        long amountPaise =
                membershipIntegrationService.getPlanAmountPaise(planName);

        if (amountPaise < 100) {
            throw new IllegalArgumentException(
                    "The membership plan has an invalid payment amount"
            );
        }

        Payment payment = Payment.createNew();
        payment.setMemberId(memberId);
        payment.setPlanName(planName);
        payment.setAmountPaise(amountPaise);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setIdempotencyKey(idempotencyKey);

        payment = paymentRepository.saveAndFlush(payment);

        try {
            RazorpayService.RazorpayOrder order =
                    razorpayService.createOrder(
                            amountPaise,
                            "gym-payment-" + payment.getId(),
                            payment.getId()
                    );

            payment.setRazorpayOrderId(order.orderId());
            payment.setStatus(PaymentStatus.PENDING);

            payment = paymentRepository.save(payment);

            return toCreatePaymentResponse(payment);
        } catch (RuntimeException exception) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Unable to create payment order");
            paymentRepository.save(payment);
            throw exception;
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(
            Long memberId,
            VerifyPaymentRequest request
    ) {
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment order not found"));

        if (!payment.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Payment order not found");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (request.getRazorpayPaymentId()
                    .equals(payment.getRazorpayPaymentId())) {
                return toPaymentResponse(payment);
            }

            throw new IllegalStateException(
                    "Payment order has already been completed"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not awaiting verification"
            );
        }

        boolean validSignature =
                razorpayService.verifyPaymentSignature(
                        payment.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature()
                );

        if (!validSignature) {
            throw new IllegalArgumentException(
                    "Payment signature verification failed"
            );
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setVerifiedAt(Instant.now());

        return toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long memberId, Long paymentId) {
        Payment payment = paymentRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found"));

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(
            Long memberId,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        return paymentRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toPaymentResponse);
    }

    private CreatePaymentResponse toCreatePaymentResponse(Payment payment) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getRazorpayOrderId(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                getRazorpayKeyId(),
                payment.getPlanName(),
                payment.getStatus().name()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPlanName(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getMembershipId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getVerifiedAt()
        );
    }

    private String getRazorpayKeyId() {
        // The public Key ID is safe to return to the checkout frontend.
        return membershipIntegrationService.getRazorpayKeyId();
    }
}
