
package com.gym.payment.service;

import com.gym.payment.config.RazorpayConfig.RazorpayCredentials;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private final RazorpayCredentials credentials;
    private final RestClient restClient;

    public RazorpayService(RazorpayCredentials credentials) {
        this.credentials = credentials;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.razorpay.com")
                .defaultHeaders(headers ->
                        headers.setBasicAuth(
                                credentials.keyId(),
                                credentials.keySecret()
                        )
                )
                .build();
    }

    public RazorpayOrder createOrder(
            Long amountPaise,
            String receipt,
            Long paymentId
    ) {
        requireCredentials();

        if (amountPaise == null || amountPaise < 100) {
            throw new IllegalArgumentException(
                    "Payment amount must be at least 100 paise"
            );
        }

        if (receipt == null || receipt.isBlank() || receipt.length() > 40) {
            throw new IllegalArgumentException(
                    "Receipt must contain between 1 and 40 characters"
            );
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("amount", amountPaise);
        request.put("currency", "INR");
        request.put("receipt", receipt);
        request.put("notes", Map.of(
                "payment_id", String.valueOf(paymentId)
        ));

        Map<String, Object> response = restClient.post()
                .uri("/v1/orders")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null
                || !(response.get("id") instanceof String orderId)
                || orderId.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay returned an invalid order response"
            );
        }

        long returnedAmount = ((Number) response.get("amount")).longValue();
        String currency = String.valueOf(response.get("currency"));
        String status = String.valueOf(response.get("status"));

        if (returnedAmount != amountPaise
                || !"INR".equals(currency)
                || !"created".equals(status)) {
            throw new IllegalStateException(
                    "Razorpay order response did not match the requested order"
            );
        }

        return new RazorpayOrder(
                orderId,
                returnedAmount,
                currency,
                status
        );
    }

    public boolean verifyPaymentSignature(
            String serverOrderId,
            String razorpayPaymentId,
            String suppliedSignature
    ) {
        requireCredentials();

        if (serverOrderId == null || serverOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()
                || suppliedSignature == null || suppliedSignature.isBlank()) {
            return false;
        }

        try {
            String message = serverOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    credentials.keySecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);

            String expectedSignature = HexFormat.of().formatHex(
                    mac.doFinal(message.getBytes(StandardCharsets.UTF_8))
            );

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    suppliedSignature.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to verify Razorpay payment signature",
                    exception
            );
        }
    }

    private void requireCredentials() {
        if (credentials.keyId() == null || credentials.keyId().isBlank()
                || credentials.keySecret() == null
                || credentials.keySecret().isBlank()) {
            throw new IllegalStateException(
                    "Razorpay credentials are not configured"
            );
        }
    }

    public record RazorpayOrder(
            String orderId,
            Long amountPaise,
            String currency,
            String status
    ) {
    }
}
