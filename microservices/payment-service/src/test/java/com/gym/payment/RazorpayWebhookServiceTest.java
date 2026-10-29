
package com.gym.payment;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.repository.PaymentRepository;
import com.gym.payment.repository.WebhookEventRepository;
import com.gym.payment.service.RazorpayWebhookService;
import com.gym.payment.service.WebhookEventRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookEventRegistrationService registrationService;

    private RazorpayWebhookService webhookService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        webhookService = new RazorpayWebhookService(
                paymentRepository,
                webhookEventRepository,
                registrationService
        );

        payment = Payment.createNew();
        payment.setMemberId(1L);
        payment.setPlanName("MONTHLY");
        payment.setAmountPaise(10000L);
        payment.setCurrency("INR");
        payment.setIdempotencyKey("test-key");
        payment.setRazorpayOrderId("order_test123");
        payment.setStatus(PaymentStatus.PENDING);
    }

    @Test
    void rejectsMissingEventId() {
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent("payment.captured");

        assertThrows(
                IllegalArgumentException.class,
                () -> webhookService.processWebhook(request, null)
        );

        verifyNoInteractions(
                paymentRepository,
                webhookEventRepository,
                registrationService
        );
    }

    @Test
    void ignoresPreviouslyRecordedEvent() {
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent("payment.captured");

        when(registrationService.registerEvent(
                "evt_duplicate",
                "payment.captured"
        )).thenReturn(false);

        webhookService.processWebhook(request, "evt_duplicate");

        verify(registrationService).registerEvent(
                "evt_duplicate",
                "payment.captured"
        );

        verifyNoInteractions(paymentRepository, webhookEventRepository);
    }

    @Test
    void rejectsUnknownOrder() throws Exception {
        PaymentWebhookRequest request = createCapturedRequest();

        when(registrationService.registerEvent(
                "evt_unknown",
                "payment.captured"
        )).thenReturn(true);

        when(paymentRepository.findByRazorpayOrderIdForUpdate(
                "order_test123"
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> webhookService.processWebhook(request, "evt_unknown")
        );

        verify(paymentRepository).findByRazorpayOrderIdForUpdate(
                "order_test123"
        );
    }

    @Test
    void capturedPaymentUpdatesStatus() throws Exception {
        PaymentWebhookRequest request = createCapturedRequest();

        WebhookEvent storedEvent = new WebhookEvent(
                "evt_capture",
                "payment.captured"
        );

        when(registrationService.registerEvent(
                "evt_capture",
                "payment.captured"
        )).thenReturn(true);

        when(paymentRepository.findByRazorpayOrderIdForUpdate(
                "order_test123"
        )).thenReturn(Optional.of(payment));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(webhookEventRepository.findByEventId("evt_capture"))
                .thenReturn(Optional.of(storedEvent));

        when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        webhookService.processWebhook(request, "evt_capture");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("pay_test123", payment.getRazorpayPaymentId());

        verify(paymentRepository).save(payment);
        verify(webhookEventRepository).save(storedEvent);
        assertEquals("PROCESSED", storedEvent.getProcessingStatus());
    }

    private PaymentWebhookRequest createCapturedRequest() throws Exception {
        String json = """
                {
                  "entity": "event",
                  "account_id": "acc_test",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test123",
                        "order_id": "order_test123",
                        "amount": 10000,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  },
                  "created_at": 1793300000
                }
                """;

        return new ObjectMapper().readValue(
                json,
                PaymentWebhookRequest.class
        );
    }
}
