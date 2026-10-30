
package com.gym.payment.service;

import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEventFailureService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventFailureService(
            WebhookEventRepository webhookEventRepository
    ) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String eventId, String reason) {

        WebhookEvent event = webhookEventRepository
                .findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Webhook event not found: " + eventId
                ));

        // Never overwrite a successfully processed event.
        if ("PROCESSED".equals(event.getProcessingStatus())) {
            return;
        }

        event.recordProcessingAttempt();
        event.markFailed(safeReason(reason));

        webhookEventRepository.save(event);
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Webhook processing failed";
        }

        return reason.length() > 1000
                ? reason.substring(0, 1000)
                : reason;
    }
}
