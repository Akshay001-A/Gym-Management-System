
package com.gym.payment.service;

import com.gym.payment.config.RazorpayConfig.RazorpayCredentials;
import com.gym.payment.exception.PaymentProcessingException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
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

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/orders")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null
                    || !(response.get("id") instanceof String orderId)
                    || orderId.isBlank()
                    || !(response.get("amount") instanceof Number amount)) {
                throw new PaymentProcessingException(
                        "Razorpay returned an invalid order response",
                        null,
                        true
                );
            }

            long returnedAmount = amount.longValue();
            String currency = String.valueOf(response.get("currency"));
            String status = String.valueOf(response.get("status"));

            if (returnedAmount != amountPaise
                    || !"INR".equals(currency)
                    || !"created".equals(status)) {
                throw new PaymentProcessingException(
                        "Razorpay order response did not match the requested order",
                        null,
                        true
                );
            }

            return new RazorpayOrder(
                    orderId,
                    returnedAmount,
                    currency,
                    status
            );

        } catch (RestClientException exception) {
            throw new PaymentProcessingException(
                    "Unable to create a Razorpay order",
                    exception,
                    !(exception instanceof RestClientResponseException response
                            && response.getStatusCode().is4xxClientError())
            );
        }
    }

    public RazorpayPayment fetchPaymentDetails(String paymentId) {
        requireCredentials();

        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is required"
            );
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri("/v1/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null
                    || !(response.get("id") instanceof String returnedId)
                    || !(response.get("amount") instanceof Number amount)) {
                throw new PaymentProcessingException(
                        "Razorpay returned an invalid payment response"
                );
            }

            return new RazorpayPayment(
                    returnedId,
                    stringValue(response.get("order_id")),
                    amount.longValue(),
                    stringValue(response.get("currency")),
                    stringValue(response.get("status"))
            );

        } catch (RestClientException exception) {
            throw new PaymentProcessingException(
                    "Unable to retrieve payment status from Razorpay",
                    exception
            );
        }
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

            byte[] expectedSignature = mac.doFinal(
                    message.getBytes(StandardCharsets.UTF_8)
            );

            byte[] actualSignature =
                    HexFormat.of().parseHex(suppliedSignature);

            return MessageDigest.isEqual(
                    expectedSignature,
                    actualSignature
            );

        } catch (Exception exception) {
            return false;
        }
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    private void requireCredentials() {
        if (credentials.keyId() == null || credentials.keyId().isBlank()
                || credentials.keySecret() == null
                || credentials.keySecret().isBlank()) {
            throw new PaymentProcessingException(
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

    public record RazorpayPayment(
            String paymentId,
            String orderId,
            Long amountPaise,
            String currency,
            String status
    ) {
    }
}
