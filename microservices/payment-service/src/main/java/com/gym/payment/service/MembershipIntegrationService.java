
package com.gym.payment.service;

import com.gym.payment.config.RazorpayConfig.RazorpayCredentials;
import com.gym.payment.exception.PaymentProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;

@Service
public class MembershipIntegrationService {

    private final RestClient restClient;
    private final RazorpayCredentials razorpayCredentials;
    private final String internalServiceKey;

    public MembershipIntegrationService(
            @Value("${membership.service.url:http://localhost:8082}")
            String membershipServiceUrl,
            RazorpayCredentials razorpayCredentials,
            @Value("${membership.internal-service.key:}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(membershipServiceUrl)
                .build();

        this.razorpayCredentials = razorpayCredentials;
        this.internalServiceKey = internalServiceKey;
    }

    public long getPlanAmountPaise(String planName) {
        return getPlanAmountPaise(planName, null);
    }

    public long getPlanAmountPaise(String planName, String memberAccessToken) {
        Map<String, Object> response = getPlan(planName, memberAccessToken);

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

    public int getPlanDurationMonths(String planName, String memberAccessToken) {
        Object durationValue = getPlan(planName, memberAccessToken).get("durationMonths");
        if (!(durationValue instanceof Number duration) || duration.intValue() < 1) {
            throw new IllegalStateException(
                    "Membership Service returned an invalid plan duration"
            );
        }
        return duration.intValue();
    }

    private Map<String, Object> getPlan(String planName, String memberAccessToken) {
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }

        Map<String, Object> response;
        try {
            var request = restClient.get()
                    .uri("/api/memberships/plans/{planName}", planName);
            if (memberAccessToken != null && !memberAccessToken.isBlank()) {
                request.header("Authorization", "Bearer " + memberAccessToken);
            }
            response = request.retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException exception) {
            throw new PaymentProcessingException(
                    "Membership Service could not provide the requested plan",
                    exception
            );
        }

        if (response == null) {
            throw new IllegalStateException(
                    "Membership Service returned an empty plan response"
            );
        }

        return response;
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

    public Long activatePaidMembership(Long paymentId, Long memberId, String planName) {
        return activatePaidMembershipWithDetails(paymentId, memberId, planName).membershipId();
    }

    public ActivatedMembership activatePaidMembershipWithDetails(Long paymentId, Long memberId, String planName) {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            throw new PaymentProcessingException("Membership activation is not configured");
        }
        Map<String, Object> request = new HashMap<>();
        request.put("paymentId", paymentId);
        request.put("memberId", memberId);
        request.put("plan", planName);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/memberships/internal/payment-activation")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object id = response == null ? null : response.get("id");
            if (!(id instanceof Number membershipId) || membershipId.longValue() <= 0) {
                throw new PaymentProcessingException("Membership Service returned no activated membership ID");
            }
            LocalDate startDate = parseDate(response.get("startDate"), "start date");
            LocalDate endDate = parseDate(response.get("endDate"), "end date");
            return new ActivatedMembership(membershipId.longValue(), startDate, endDate);
        } catch (RestClientException exception) {
            throw new PaymentProcessingException("Membership Service could not activate the paid plan", exception);
        }
    }

    private LocalDate parseDate(Object value, String label) {
        if (value == null) throw new PaymentProcessingException("Membership Service returned no membership " + label);
        try {
            return LocalDate.parse(value.toString());
        } catch (RuntimeException exception) {
            throw new PaymentProcessingException("Membership Service returned an invalid membership " + label, exception);
        }
    }

    public record ActivatedMembership(Long membershipId, LocalDate startDate, LocalDate endDate) { }

    public void ensureMemberCanChangePlan(String memberAccessToken) {
        if (memberAccessToken == null || memberAccessToken.isBlank()) {
            throw new PaymentProcessingException("Member identity is required to check current membership");
        }
        try {
            List<Map<String, Object>> memberships = restClient.get()
                    .uri("/api/memberships/me")
                    .header("Authorization", "Bearer " + memberAccessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (memberships == null) return;
            LocalDate today = LocalDate.now();
            boolean hasCurrent = memberships.stream().anyMatch(membership ->
                    "ACTIVE".equalsIgnoreCase(String.valueOf(membership.get("status")))
                    && membership.get("endDate") != null
                    && !LocalDate.parse(membership.get("endDate").toString()).isBefore(today));
            if (hasCurrent) {
                throw new IllegalStateException("Cancel your current membership or wait until it expires before choosing a different plan.");
            }
        } catch (RestClientException exception) {
            throw new PaymentProcessingException("Membership Service could not check your current membership", exception);
        }
    }
}
