
package com.gym.payment.service;

import com.gym.payment.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEventRegistrationService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventRegistrationService(
            WebhookEventRepository webhookEventRepository
    ) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registerEvent(String eventId, String eventType) {

        if (eventId == null || eventId.isBlank()
                || eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook event ID and type are required"
            );
        }

        return webhookEventRepository.insertIfAbsent(
                eventId,
                eventType
        ) == 1;
    }
}
