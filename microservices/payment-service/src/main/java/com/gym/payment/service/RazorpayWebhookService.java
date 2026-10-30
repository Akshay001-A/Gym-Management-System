
package com.gym.payment.service;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.repository.PaymentRepository;
import com.gym.payment.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RazorpayWebhookService {

    private final WebhookEventRegistrationService registrationService;
    private final RazorpayWebhookProcessor processor;
    private final WebhookEventFailureService failureService;
    private final PaymentRepository paymentRepository;
    private final PaymentEmailService paymentEmailService;

    @Autowired
    public RazorpayWebhookService(
            WebhookEventRegistrationService registrationService,
            RazorpayWebhookProcessor processor,
            WebhookEventFailureService failureService,
            PaymentRepository paymentRepository,
            PaymentEmailService paymentEmailService
    ) {
        this.registrationService = registrationService;
        this.processor = processor;
        this.failureService = failureService;
        this.paymentRepository = paymentRepository;
        this.paymentEmailService = paymentEmailService;
    }

    public RazorpayWebhookService(WebhookEventRegistrationService registrationService,
            RazorpayWebhookProcessor processor, WebhookEventFailureService failureService) {
        this(registrationService, processor, failureService, null, null);
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
            if (paymentRepository != null && paymentEmailService != null
                    && "payment.captured".equals(webhookRequest.getEvent())) {
                var entity = webhookRequest.getPayload().path("payment").path("entity");
                var orderValue = entity.path("order_id");
                if (!orderValue.isMissingNode() && !orderValue.isNull() && orderValue.isString()) {
                    paymentRepository.findByRazorpayOrderId(orderValue.asString())
                            .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                            .ifPresent(payment -> paymentEmailService.sendReceiptIfNeeded(payment.getId()));
                }
            }

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
