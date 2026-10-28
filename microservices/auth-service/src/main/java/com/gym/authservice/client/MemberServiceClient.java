package com.gym.authservice.client;

import com.gym.authservice.dto.MemberProfileRequest;
import com.gym.authservice.dto.MemberProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MemberServiceClient {
    private final RestClient restClient;
    private final String internalServiceKey;

    public MemberServiceClient(RestClient.Builder builder,
            @Value("${app.member-service.url:http://localhost:8081}") String memberServiceUrl,
            @Value("${app.internal-service.key:}") String internalServiceKey) {
        this.restClient = builder.baseUrl(memberServiceUrl).build();
        this.internalServiceKey = internalServiceKey;
    }

    public Long createMemberProfile(MemberProfileRequest request, String registrationKey) {
        try {
            MemberProfileResponse response = restClient.post()
                    .uri("/api/members/internal")
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .header("X-Registration-Key", registrationKey)
                    .body(request)
                    .retrieve()
                    .body(MemberProfileResponse.class);
            if (response == null || response.id() == null) {
                throw new IllegalStateException("Member Service returned no profile ID");
            }
            return response.id();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Member Service could not create the profile", ex);
        }
    }

    public void deleteMemberProfile(Long memberId) {
        try {
            restClient.delete()
                    .uri("/api/members/internal/{id}", memberId)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Member Service could not remove the unlinked profile", ex);
        }
    }
}
