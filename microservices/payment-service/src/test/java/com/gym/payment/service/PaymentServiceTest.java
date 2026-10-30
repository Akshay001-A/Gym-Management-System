
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayService razorpayService;

    @Mock
    private MembershipIntegrationService membershipIntegrationService;

    @Mock
    private PaymentOrderPersistenceService orderPersistenceService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment initialPayment;
    private Payment updatedPayment;

    @BeforeEach
    void setUp() {
        initialPayment = mock(Payment.class);
        updatedPayment = mock(Payment.class);

        lenient().when(initialPayment.getId()).thenReturn(1L);

        lenient().when(updatedPayment.getId()).thenReturn(1L);
        lenient().when(updatedPayment.getRazorpayOrderId())
                .thenReturn("order_test_123");
        lenient().when(updatedPayment.getAmountPaise())
                .thenReturn(150000L);
        lenient().when(updatedPayment.getCurrency())
                .thenReturn("INR");
        lenient().when(updatedPayment.getPlanName())
                .thenReturn("MONTHLY");
        lenient().when(updatedPayment.getStatus())
                .thenReturn(PaymentStatus.PENDING);

        lenient().when(membershipIntegrationService.getRazorpayKeyId())
                .thenReturn("rzp_test_public_key");
    }

    @Test
    void createPaymentOrderShouldCreateOrderSuccessfully() {
        when(orderPersistenceService.findByIdempotencyKey("key-001"))
                .thenReturn(null);

        when(membershipIntegrationService.getPlanAmountPaise("MONTHLY"))
                .thenReturn(150000L);

        when(orderPersistenceService.createPayment(
                101L, "MONTHLY", 150000L, "key-001"
        )).thenReturn(initialPayment);

        when(razorpayService.createOrder(
                150000L, "gym-payment-1", 1L
        )).thenReturn(new RazorpayService.RazorpayOrder(
                "order_test_123", 150000L, "INR", "created"
        ));

        when(orderPersistenceService.markOrderCreated(
                1L, "order_test_123"
        )).thenReturn(updatedPayment);

        var response = paymentService.createPaymentOrder(
                101L, "MONTHLY", "key-001"
        );

        assertNotNull(response);

        verify(orderPersistenceService).createPayment(
                101L, "MONTHLY", 150000L, "key-001"
        );

        verify(razorpayService).createOrder(
                150000L, "gym-payment-1", 1L
        );

        verify(orderPersistenceService).markOrderCreated(
                1L, "order_test_123"
        );

        verify(orderPersistenceService, never())
                .markOrderFailed(anyLong(), anyString());
    }

    @Test
    void createPaymentOrderShouldPersistFailureWhenRazorpayFails() {
        when(orderPersistenceService.findByIdempotencyKey("key-002"))
                .thenReturn(null);

        when(membershipIntegrationService.getPlanAmountPaise("MONTHLY"))
                .thenReturn(150000L);

        when(orderPersistenceService.createPayment(
                101L, "MONTHLY", 150000L, "key-002"
        )).thenReturn(initialPayment);

        when(razorpayService.createOrder(
                150000L, "gym-payment-1", 1L
        )).thenThrow(new RuntimeException("Razorpay unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-002"
                )
        );

        assertEquals("Razorpay unavailable", exception.getMessage());

        verify(orderPersistenceService).markOrderFailed(
                1L, "Razorpay unavailable"
        );

        verify(orderPersistenceService, never())
                .markOrderCreated(anyLong(), anyString());
    }

    @Test
    void createPaymentOrderShouldReturnExistingOrderForSameKey() {
        Payment existingPayment = mock(Payment.class);

        when(existingPayment.getMemberId()).thenReturn(101L);
        when(existingPayment.getPlanName()).thenReturn("MONTHLY");
        when(existingPayment.getRazorpayOrderId())
                .thenReturn("order_existing_123");
        when(existingPayment.getId()).thenReturn(5L);
        when(existingPayment.getAmountPaise()).thenReturn(150000L);
        when(existingPayment.getCurrency()).thenReturn("INR");
        when(existingPayment.getStatus()).thenReturn(PaymentStatus.PENDING);

        when(orderPersistenceService.findByIdempotencyKey("key-existing"))
                .thenReturn(existingPayment);

        var response = paymentService.createPaymentOrder(
                101L, "MONTHLY", "key-existing"
        );

        assertNotNull(response);

        verify(razorpayService, never())
                .createOrder(anyLong(), anyString(), anyLong());

        verify(orderPersistenceService, never())
                .createPayment(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void createPaymentOrderShouldRejectKeyUsedForDifferentMember() {
        Payment existingPayment = mock(Payment.class);

        when(existingPayment.getMemberId()).thenReturn(999L);

        when(orderPersistenceService.findByIdempotencyKey("key-used"))
                .thenReturn(existingPayment);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-used"
                )
        );

        verifyNoInteractions(razorpayService);

        verify(orderPersistenceService, never())
                .createPayment(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void createPaymentOrderShouldRejectKeyUsedForDifferentPlan() {
        Payment existingPayment = mock(Payment.class);

        when(existingPayment.getMemberId()).thenReturn(101L);
        when(existingPayment.getPlanName()).thenReturn("YEARLY");

        when(orderPersistenceService.findByIdempotencyKey("key-used"))
                .thenReturn(existingPayment);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-used"
                )
        );

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectInvalidMemberId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        0L, "MONTHLY", "key-003"
                )
        );

        verifyNoInteractions(orderPersistenceService);
        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectBlankPlanName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        101L, " ", "key-004"
                )
        );

        verifyNoInteractions(orderPersistenceService);
        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectBlankIdempotencyKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", " "
                )
        );

        verifyNoInteractions(orderPersistenceService);
        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectInvalidPlanAmount() {
        when(orderPersistenceService.findByIdempotencyKey("key-005"))
                .thenReturn(null);

        when(membershipIntegrationService.getPlanAmountPaise("MONTHLY"))
                .thenReturn(50L);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-005"
                )
        );

        verify(orderPersistenceService, never())
                .createPayment(anyLong(), anyString(), anyLong(), anyString());

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectExistingRequestWithoutOrderId() {
        Payment existingPayment = mock(Payment.class);

        when(existingPayment.getMemberId()).thenReturn(101L);
        when(existingPayment.getPlanName()).thenReturn("MONTHLY");
        when(existingPayment.getRazorpayOrderId()).thenReturn(null);

        when(orderPersistenceService.findByIdempotencyKey("key-processing"))
                .thenReturn(existingPayment);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-processing"
                )
        );

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrderShouldRejectInvalidRazorpayOrderResponse() {
        when(orderPersistenceService.findByIdempotencyKey("key-006"))
                .thenReturn(null);

        when(membershipIntegrationService.getPlanAmountPaise("MONTHLY"))
                .thenReturn(150000L);

        when(orderPersistenceService.createPayment(
                101L, "MONTHLY", 150000L, "key-006"
        )).thenReturn(initialPayment);

        when(razorpayService.createOrder(
                150000L, "gym-payment-1", 1L
        )).thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.createPaymentOrder(
                        101L, "MONTHLY", "key-006"
                )
        );

        verify(orderPersistenceService).markOrderFailed(
                1L,
                "Razorpay returned an invalid order"
        );
    }
}
