
package com.gym.payment;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.service.RazorpayWebhookProcessor;
import com.gym.payment.service.RazorpayWebhookService;
import com.gym.payment.service.WebhookEventFailureService;
import com.gym.payment.service.WebhookEventRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    @Mock
    private WebhookEventRegistrationService registrationService;

    @Mock
    private RazorpayWebhookProcessor processor;

    @Mock
    private WebhookEventFailureService failureService;

    private RazorpayWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new RazorpayWebhookService(
                registrationService,
                processor,
                failureService
        );
    }

    @Test
    void rejectsMissingEventId() {

        PaymentWebhookRequest request = createRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> webhookService.processWebhook(request, null)
        );

        verifyNoInteractions(
                registrationService,
                processor,
                failureService
        );
    }

    @Test
    void rejectsMissingEventType() {

        PaymentWebhookRequest request = new PaymentWebhookRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> webhookService.processWebhook(
                        request,
                        "evt_missing_type"
                )
        );

        verifyNoInteractions(
                registrationService,
                processor,
                failureService
        );
    }

    @Test
    void processesNewEvent() {

        PaymentWebhookRequest request = createRequest();

        when(registrationService.registerEvent(
                "evt_new",
                "payment.captured"
        )).thenReturn(true);

        webhookService.processWebhook(request, "evt_new");

        verify(registrationService).registerEvent(
                "evt_new",
                "payment.captured"
        );

        verify(processor).process(request, "evt_new");
        verifyNoInteractions(failureService);
    }

    @Test
    void retriesPreviouslyRegisteredEvent() {

        PaymentWebhookRequest request = createRequest();

        when(registrationService.registerEvent(
                "evt_retry",
                "payment.captured"
        )).thenReturn(false);

        webhookService.processWebhook(request, "evt_retry");

        // Existing event is inspected by the transactional processor.
        verify(processor).process(request, "evt_retry");
    }

    @Test
    void recordsFailureAndRethrowsProcessingException() {

        PaymentWebhookRequest request = createRequest();

        RuntimeException processingException =
                new RuntimeException("Temporary database failure");

        when(registrationService.registerEvent(
                "evt_failed",
                "payment.captured"
        )).thenReturn(true);

        doThrow(processingException)
                .when(processor)
                .process(request, "evt_failed");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> webhookService.processWebhook(
                        request,
                        "evt_failed"
                )
        );

        org.junit.jupiter.api.Assertions.assertSame(
                processingException,
                thrown
        );

        verify(failureService).markFailed(
                "evt_failed",
                "Temporary database failure"
        );
    }

    private PaymentWebhookRequest createRequest() {

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent("payment.captured");

        return request;
    }
}
