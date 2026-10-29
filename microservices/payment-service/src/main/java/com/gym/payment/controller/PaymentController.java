package com.gym.payment.controller;

import com.gym.payment.dto.CreatePaymentRequest;
import com.gym.payment.dto.CreatePaymentResponse;
import com.gym.payment.dto.PaymentResponse;
import com.gym.payment.dto.VerifyPaymentRequest;
import com.gym.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create a Razorpay order for a membership plan.
     */
    @PostMapping("/orders")
    public ResponseEntity<CreatePaymentResponse> createPaymentOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey
    ) {
        Long memberId = getMemberId(jwt);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        CreatePaymentResponse response =
                paymentService.createPaymentOrder(
                        memberId,
                        request.getPlanName(),
                        idempotencyKey
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verify the payment signature returned by Razorpay Checkout.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        Long memberId = getMemberId(jwt);

        PaymentResponse response =
                paymentService.verifyPayment(memberId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve a payment belonging to the authenticated member.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long paymentId
    ) {
        Long memberId = getMemberId(jwt);

        PaymentResponse response =
                paymentService.getPayment(memberId, paymentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve paginated payment history for the authenticated member.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = getMemberId(jwt);

        Page<PaymentResponse> response =
                paymentService.getPaymentHistory(memberId, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract the member ID from the authenticated JWT.
     */
    private Long getMemberId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalStateException(
                    "Authenticated JWT is missing"
            );
        }

        Object memberIdClaim = jwt.getClaims().get("memberId");

        if (memberIdClaim instanceof Number number) {
            long memberId = number.longValue();

            if (memberId > 0) {
                return memberId;
            }
        }

        if (memberIdClaim instanceof String value) {
            try {
                long memberId = Long.parseLong(value);

                if (memberId > 0) {
                    return memberId;
                }
            } catch (NumberFormatException ignored) {
                // Handle the missing or invalid member ID below.
            }
        }

        throw new IllegalArgumentException(
                "The authenticated user does not have a valid member ID"
        );
    }
}
