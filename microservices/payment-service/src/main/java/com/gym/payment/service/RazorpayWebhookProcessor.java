
package com.gym.payment.service;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.repository.PaymentRepository;
import com.gym.payment.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import com.gym.payment.exception.WebhookRetryableException;

import java.time.Instant;

@Service
public class RazorpayWebhookProcessor {

    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final MembershipIntegrationService membershipIntegrationService;

    @Autowired
    public RazorpayWebhookProcessor(
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            MembershipIntegrationService membershipIntegrationService
    ) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.membershipIntegrationService = membershipIntegrationService;
    }

    public RazorpayWebhookProcessor(PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository) {
        this(paymentRepository, webhookEventRepository, null);
    }

    @Transactional
    public void process(
            PaymentWebhookRequest request,
            String eventId
    ) {
        WebhookEvent storedEvent = webhookEventRepository
                .findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registered webhook event was not found"
                ));

        // A previous delivery already completed successfully.
        if ("PROCESSED".equals(storedEvent.getProcessingStatus())) {
            return;
        }

        if (!storedEvent.getEventType().equals(request.getEvent())) {
            throw new IllegalArgumentException(
                    "Webhook event type does not match registered event"
            );
        }

        processPaymentEvent(request);

        storedEvent.recordProcessingAttempt();
        storedEvent.markProcessed();

        webhookEventRepository.save(storedEvent);
    }

    private void processPaymentEvent(PaymentWebhookRequest request) {

        String event = request.getEvent();

        // Ignore other valid Razorpay event types.
        if (!"payment.captured".equals(event)
                && !"payment.failed".equals(event)) {
            return;
        }

        JsonNode paymentEntity = request.getPayload()
                .path("payment")
                .path("entity");

        String orderId = textValue(paymentEntity, "order_id");
        String paymentId = textValue(paymentEntity, "id");
        Long amountPaise = longValue(paymentEntity, "amount");
        String currency = textValue(paymentEntity, "currency");

        if (orderId == null || paymentId == null
                || amountPaise == null || currency == null) {
            throw new IllegalArgumentException(
                    "Webhook payment details are incomplete or invalid"
            );
        }

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new WebhookRetryableException(
                        "Payment order is not available yet. Webhook retry required."
                ));


        if (!payment.getAmountPaise().equals(amountPaise)
                || !"INR".equals(currency)
                || !currency.equals(payment.getCurrency())) {
            throw new IllegalArgumentException(
                    "Webhook payment details do not match the stored order"
            );
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (paymentId.equals(payment.getRazorpayPaymentId())) {
                return;
            }

            throw new IllegalArgumentException(
                    "Payment already completed with a different payment ID"
            );
        }

        if (payment.getStatus() == PaymentStatus.FAILED
                && "payment.failed".equals(event)) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.CREATED
                && payment.getStatus() != PaymentStatus.FAILED) {
            return;
        }

        if ("payment.captured".equals(event)) {

            // A captured payment can supersede an earlier failed attempt
            // for the same order.
            payment.setRazorpayPaymentId(paymentId);
            MembershipIntegrationService.ActivatedMembership activation = membershipIntegrationService.activatePaidMembershipWithDetails(
                    payment.getId(), payment.getMemberId(), payment.getPlanName());
            payment.setMembershipId(activation.membershipId());
            payment.setMembershipStartDate(activation.startDate());
            payment.setMembershipEndDate(activation.endDate());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            payment.setVerifiedAt(Instant.now());

        } else {

            // Do not replace an existing successful payment.
            payment.setRazorpayPaymentId(paymentId);

            String reason = textValue(
                    paymentEntity,
                    "error_description"
            );

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(
                    reason == null ? "Payment failed" : reason
            );
        }

        paymentRepository.save(payment);
    }

    private String textValue(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()
                || !value.isString()) {
            return null;
        }

        String result = value.asString();

        return result == null || result.isBlank() ? null : result;
    }

    private Long longValue(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()
                || !value.isNumber()) {
            return null;
        }

        long result = value.asLong();

        if (result <= 0 || value.doubleValue() != (double) result) {
            return null;
        }

        return result;
    }
}
