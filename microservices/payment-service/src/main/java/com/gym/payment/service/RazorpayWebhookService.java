
package com.gym.payment.service;

import com.gym.payment.dto.PaymentWebhookRequest;
import org.springframework.stereotype.Service;

@Service
public class RazorpayWebhookService {

    private final WebhookEventRegistrationService registrationService;
    private final RazorpayWebhookProcessor processor;
    private final WebhookEventFailureService failureService;

    public RazorpayWebhookService(
            WebhookEventRegistrationService registrationService,
            RazorpayWebhookProcessor processor,
            WebhookEventFailureService failureService
    ) {
        this.registrationService = registrationService;
        this.processor = processor;
        this.failureService = failureService;
    }

    public void processWebhook(
            PaymentWebhookRequest webhookRequest,
            String eventId
    ) {
        if (webhookRequest == null
                || webhookRequest.getEvent() == null
                || webhookRequest.getEvent().isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook event is missing"
            );
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay event ID is required"
            );
        }

        // Register the event, or leave the existing event available
        // for the processor to inspect and possibly retry.
        registrationService.registerEvent(
                eventId,
                webhookRequest.getEvent()
        );

        try {
            processor.process(webhookRequest, eventId);

        } catch (RuntimeException exception) {

            // Record failure in a separate transaction.
            try {
                failureService.markFailed(
                        eventId,
                        exception.getMessage()
                );
            } catch (RuntimeException failureException) {
                exception.addSuppressed(failureException);
            }

            throw exception;
        }
    }
}
