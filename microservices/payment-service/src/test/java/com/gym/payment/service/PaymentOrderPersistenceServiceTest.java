
package com.gym.payment.service;

import com.gym.payment.entity.Payment;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderPersistenceServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentOrderPersistenceService persistenceService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.createNew();
        payment.setMemberId(101L);
        payment.setPlanName("MONTHLY");
        payment.setAmountPaise(150000L);
        payment.setCurrency("INR");
        payment.setIdempotencyKey("test-key-001");
        payment.setStatus(PaymentStatus.CREATED);
    }

    @Test
    void createPaymentShouldSaveCreatedPayment() {
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = persistenceService.createPayment(
                101L,
                "MONTHLY",
                150000L,
                "test-key-001"
        );

        assertNotNull(result);
        assertEquals(101L, result.getMemberId());
        assertEquals("MONTHLY", result.getPlanName());
        assertEquals(150000L, result.getAmountPaise());
        assertEquals("INR", result.getCurrency());
        assertEquals(PaymentStatus.CREATED, result.getStatus());
        assertEquals("test-key-001", result.getIdempotencyKey());

        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    void markOrderCreatedShouldSetPendingStatus() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = persistenceService.markOrderCreated(
                1L,
                "order_test_123"
        );

        assertEquals("order_test_123", result.getRazorpayOrderId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNull(result.getFailureReason());

        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void markOrderCreatedShouldRejectBlankOrderId() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.markOrderCreated(1L, " ")
        );

        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void markOrderFailedShouldPersistFailure() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        persistenceService.markOrderFailed(
                1L,
                "Razorpay service unavailable"
        );

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(
                "Razorpay service unavailable",
                payment.getFailureReason()
        );

        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void markOrderFailedShouldUseDefaultReasonWhenNull() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        persistenceService.markOrderFailed(1L, null);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(
                "Payment order creation failed",
                payment.getFailureReason()
        );
    }

    @Test
    void markOrderFailedShouldLimitReasonTo500Characters() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String longReason = "X".repeat(700);

        persistenceService.markOrderFailed(1L, longReason);

        assertEquals(500, payment.getFailureReason().length());
    }

    @Test
    void markOrderFailedShouldNotOverwritePendingPayment() {
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId("order_test_123");

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        persistenceService.markOrderFailed(1L, "Some failure");

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("order_test_123", payment.getRazorpayOrderId());

        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void markOrderFailedShouldNotOverwriteSuccessfulPayment() {
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        persistenceService.markOrderFailed(1L, "Some failure");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());

        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void findByIdempotencyKeyShouldReturnExistingPayment() {
        when(paymentRepository.findByIdempotencyKey("test-key-001"))
                .thenReturn(Optional.of(payment));

        Payment result = persistenceService.findByIdempotencyKey(
                "test-key-001"
        );

        assertNotNull(result);
        assertEquals("test-key-001", result.getIdempotencyKey());
    }

    @Test
    void findByIdempotencyKeyShouldReturnNullWhenMissing() {
        when(paymentRepository.findByIdempotencyKey("missing-key"))
                .thenReturn(Optional.empty());

        Payment result = persistenceService.findByIdempotencyKey(
                "missing-key"
        );

        assertNull(result);
    }

    @Test
    void persistenceMethodsShouldUseIndependentTransactions() throws Exception {
        Transactional createAnnotation =
                PaymentOrderPersistenceService.class
                        .getMethod(
                                "createPayment",
                                Long.class,
                                String.class,
                                long.class,
                                String.class
                        )
                        .getAnnotation(Transactional.class);

        Transactional updateAnnotation =
                PaymentOrderPersistenceService.class
                        .getMethod(
                                "markOrderCreated",
                                Long.class,
                                String.class
                        )
                        .getAnnotation(Transactional.class);

        Transactional failureAnnotation =
                PaymentOrderPersistenceService.class
                        .getMethod(
                                "markOrderFailed",
                                Long.class,
                                String.class
                        )
                        .getAnnotation(Transactional.class);

        assertNotNull(createAnnotation);
        assertNotNull(updateAnnotation);
        assertNotNull(failureAnnotation);

        assertEquals(Propagation.REQUIRES_NEW, createAnnotation.propagation());
        assertEquals(Propagation.REQUIRES_NEW, updateAnnotation.propagation());
        assertEquals(Propagation.REQUIRES_NEW, failureAnnotation.propagation());
    }
}
