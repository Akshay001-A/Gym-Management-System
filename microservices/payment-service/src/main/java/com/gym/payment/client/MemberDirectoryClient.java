package com.gym.payment.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MemberDirectoryClient {
    private final RestClient restClient;
    private final String internalServiceKey;

    public MemberDirectoryClient(
            @Value("${app.member-service.url:http://localhost:8081}") String memberServiceUrl,
            @Value("${app.internal-service.key:}") String internalServiceKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(3));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(memberServiceUrl)
                .build();
        this.internalServiceKey = internalServiceKey;
    }

    public Map<Long, MemberProfile> getProfiles(List<Long> memberIds) {
        if (memberIds.isEmpty()) return Map.of();
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            throw new IllegalStateException("Internal member lookup is not configured");
        }
        List<MemberProfile> profiles = restClient.post()
                .uri("/api/members/internal/batch")
                .header("X-Internal-Service-Key", internalServiceKey)
                .body(Map.of("memberIds", memberIds))
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        if (profiles == null) return Map.of();
        return profiles.stream().collect(Collectors.toMap(MemberProfile::id, Function.identity(), (first, ignored) -> first));
    }

    public record MemberProfile(Long id, String name, String email) { }
}
