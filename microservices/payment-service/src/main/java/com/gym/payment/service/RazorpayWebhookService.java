
package com.gym.payment.service;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.repository.PaymentRepository;
import com.gym.payment.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Service
public class RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookEventRegistrationService registrationService;

    public RazorpayWebhookService(
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            WebhookEventRegistrationService registrationService
    ) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.registrationService = registrationService;
    }

    public void processWebhook(
            PaymentWebhookRequest webhookRequest,
            String eventId
    ) {
        if (webhookRequest == null
                || webhookRequest.getEvent() == null
                || webhookRequest.getEvent().isBlank()) {
            throw new IllegalArgumentException("Webhook event is missing");
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay event ID is required"
            );
        }

        boolean registered = registrationService.registerEvent(
                eventId,
                webhookRequest.getEvent()
        );

        if (!registered) {
            return;
        }

        processRegisteredWebhook(webhookRequest, eventId);
    }

    @Transactional
    protected void processRegisteredWebhook(
            PaymentWebhookRequest webhookRequest,
            String eventId
    ) {
        processPaymentEvent(webhookRequest);

        WebhookEvent storedEvent = webhookEventRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registered webhook event was not found"
                ));

        storedEvent.setProcessingStatus("PROCESSED");
        storedEvent.setProcessedAt(Instant.now());

        webhookEventRepository.save(storedEvent);
    }

    private void processPaymentEvent(PaymentWebhookRequest webhookRequest) {
        String event = webhookRequest.getEvent();

        if (!event.equals("payment.captured")
                && !event.equals("payment.failed")) {
            return;
        }

        JsonNode paymentEntity = webhookRequest.getPayload()
                .path("payment")
                .path("entity");

        String orderId = textValue(paymentEntity, "order_id");
        String paymentId = textValue(paymentEntity, "id");
        Long amountPaise = longValue(paymentEntity, "amount");
        String currency = textValue(paymentEntity, "currency");

        if (orderId == null || paymentId == null || amountPaise == null) {
            throw new IllegalArgumentException(
                    "Webhook payment details are incomplete"
            );
        }

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment order was not found"
                ));

        if (!payment.getAmountPaise().equals(amountPaise)
                || !"INR".equals(currency)
                || !payment.getCurrency().equals(currency)) {
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
                && event.equals("payment.failed")) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.CREATED) {
            return;
        }

        if (event.equals("payment.captured")) {
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            payment.setVerifiedAt(Instant.now());
        } else {
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

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        String text = value.asString();

        return text == null || text.isBlank() ? null : text;
    }

    private Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        try {
            return value.asLong();
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
