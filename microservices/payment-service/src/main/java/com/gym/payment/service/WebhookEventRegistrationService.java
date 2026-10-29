
package com.gym.payment.service;

import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.repository.WebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        if (webhookEventRepository.existsByEventId(eventId)) {
            return false;
        }

        try {
            webhookEventRepository.saveAndFlush(
                    new WebhookEvent(eventId, eventType)
            );
            return true;
        } catch (DataIntegrityViolationException exception) {
            // A concurrent request may have registered this event first.
            return false;
        }
    }
}
