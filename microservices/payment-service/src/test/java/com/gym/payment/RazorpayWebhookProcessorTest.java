
package com.gym.payment;

import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.entity.WebhookEvent;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.exception.WebhookRetryableException;
import com.gym.payment.repository.PaymentRepository;
import com.gym.payment.repository.WebhookEventRepository;
import com.gym.payment.service.RazorpayWebhookProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    private RazorpayWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RazorpayWebhookProcessor(
                paymentRepository,
                webhookEventRepository
        );
    }

    @Test
    void throwsRetryableExceptionWhenPaymentOrderIsNotFound() {

        PaymentWebhookRequest request = createRequest(
                "order_123",
                "pay_123",
                10000L,
                "INR"
        );

        WebhookEvent event = new WebhookEvent(
                "evt_unknown",
                "payment.captured"
        );

        when(webhookEventRepository.findByEventIdForUpdate("evt_unknown"))
                .thenReturn(Optional.of(event));

        when(paymentRepository.findByRazorpayOrderIdForUpdate("order_123"))
                .thenReturn(Optional.empty());

        assertThrows(
                WebhookRetryableException.class,
                () -> processor.process(request, "evt_unknown")
        );

        verify(paymentRepository)
                .findByRazorpayOrderIdForUpdate("order_123");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsPaymentWhenAmountDoesNotMatch() {

        PaymentWebhookRequest request = createRequest(
                "order_123",
                "pay_123",
                10000L,
                "INR"
        );

        WebhookEvent event = new WebhookEvent(
                "evt_amount",
                "payment.captured"
        );

        Payment payment = createPayment();
        payment.setAmountPaise(20000L);

        when(webhookEventRepository.findByEventIdForUpdate("evt_amount"))
                .thenReturn(Optional.of(event));

        when(paymentRepository.findByRazorpayOrderIdForUpdate("order_123"))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(request, "evt_amount")
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsPaymentWhenCurrencyDoesNotMatch() {

        PaymentWebhookRequest request = createRequest(
                "order_123",
                "pay_123",
                10000L,
                "USD"
        );

        WebhookEvent event = new WebhookEvent(
                "evt_currency",
                "payment.captured"
        );

        Payment payment = createPayment();

        when(webhookEventRepository.findByEventIdForUpdate("evt_currency"))
                .thenReturn(Optional.of(event));

        when(paymentRepository.findByRazorpayOrderIdForUpdate("order_123"))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(request, "evt_currency")
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void ignoresAlreadyProcessedWebhookEvent() {

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent("payment.captured");

        WebhookEvent event = new WebhookEvent(
                "evt_duplicate",
                "payment.captured"
        );

        event.setProcessingStatus("PROCESSED");

        when(webhookEventRepository.findByEventIdForUpdate("evt_duplicate"))
                .thenReturn(Optional.of(event));

        assertDoesNotThrow(
                () -> processor.process(request, "evt_duplicate")
        );

        verifyNoInteractions(paymentRepository);

        verify(webhookEventRepository, never())
                .save(any());
    }

    private Payment createPayment() {

        Payment payment = Payment.createNew();

        payment.setMemberId(1L);
        payment.setPlanName("MONTHLY");
        payment.setAmountPaise(10000L);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId("order_123");
        payment.setIdempotencyKey("test-key");

        return payment;
    }

    private PaymentWebhookRequest createRequest(
            String orderId,
            String paymentId,
            Long amount,
            String currency
    ) {

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent("payment.captured");

        JsonNode payload = mock(JsonNode.class);
        JsonNode paymentNode = mock(JsonNode.class);
        JsonNode entityNode = mock(JsonNode.class);

        when(payload.path("payment")).thenReturn(paymentNode);
        when(paymentNode.path("entity")).thenReturn(entityNode);

        stubString(entityNode, "order_id", orderId);
        stubString(entityNode, "id", paymentId);
        stubString(entityNode, "currency", currency);

        JsonNode amountNode = mock(JsonNode.class);

        when(entityNode.path("amount")).thenReturn(amountNode);
        when(amountNode.isMissingNode()).thenReturn(false);
        when(amountNode.isNull()).thenReturn(false);
        when(amountNode.isNumber()).thenReturn(true);
        when(amountNode.asLong()).thenReturn(amount);
        when(amountNode.doubleValue()).thenReturn(amount.doubleValue());

        request.setPayload(payload);

        return request;
    }

    private void stubString(
            JsonNode parent,
            String field,
            String value
    ) {

        JsonNode node = mock(JsonNode.class);

        when(parent.path(field)).thenReturn(node);
        when(node.isMissingNode()).thenReturn(false);
        when(node.isNull()).thenReturn(false);
        when(node.isString()).thenReturn(true);
        when(node.asString()).thenReturn(value);
    }
}
