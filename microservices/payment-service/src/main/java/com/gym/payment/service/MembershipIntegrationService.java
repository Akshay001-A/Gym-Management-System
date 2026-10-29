
package com.gym.payment.service;

import com.gym.payment.config.RazorpayConfig.RazorpayCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class MembershipIntegrationService {

    private final RestClient restClient;
    private final RazorpayCredentials razorpayCredentials;

    public MembershipIntegrationService(
            @Value("${membership.service.url:http://localhost:8082}")
            String membershipServiceUrl,
            RazorpayCredentials razorpayCredentials
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(membershipServiceUrl)
                .build();

        this.razorpayCredentials = razorpayCredentials;
    }

    public long getPlanAmountPaise(String planName) {
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }

        Map<String, Object> response = restClient.get()
                .uri("/api/memberships/plans/{planName}", planName)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException(
                    "Membership Service returned an empty plan response"
            );
        }

        Object priceValue = response.get("price");

        if (priceValue == null) {
            throw new IllegalStateException(
                    "Membership Service response does not contain a plan price"
            );
        }

        BigDecimal price;

        try {
            price = new BigDecimal(priceValue.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Membership Service returned an invalid plan price",
                    exception
            );
        }

        if (price.signum() <= 0
                || price.scale() > 2
                || price.precision() > 12) {
            throw new IllegalStateException(
                    "Membership Service returned an invalid plan price"
            );
        }

        return price.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    public String getRazorpayKeyId() {
        if (razorpayCredentials.keyId() == null
                || razorpayCredentials.keyId().isBlank()) {
            throw new IllegalStateException(
                    "Razorpay Key ID is not configured"
            );
        }

        return razorpayCredentials.keyId();
    }
}
