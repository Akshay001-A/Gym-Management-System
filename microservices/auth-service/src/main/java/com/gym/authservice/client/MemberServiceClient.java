
package com.gym.authservice.client;

import com.gym.authservice.dto.MemberProfileRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MemberServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(MemberServiceClient.class);

    private final RestClient restClient;

    public MemberServiceClient(
            RestClient.Builder builder,
            @Value("${app.member-service.url:http://localhost:8081}")
            String memberServiceUrl) {

        this.restClient = builder
                .baseUrl(memberServiceUrl)
                .build();
    }

    /**
     * Creates a member profile through Member Service.
     */
    public void createMemberProfile(MemberProfileRequest request) {
        try {
            restClient.post()
                    .uri("/api/members")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Member profile created successfully");

        } catch (RestClientResponseException ex) {

            String responseBody = ex.getResponseBodyAsString();

            log.warn(
                    "Member Service rejected profile. HTTP status: {}, "
                            + "Response: {}",
                    ex.getStatusCode().value(),
                    responseBody
            );

            // Keep the original exception as the cause.
            // AuthService can inspect its response body.
            throw new IllegalStateException(
                    "Member Service rejected the profile. HTTP "
                            + ex.getStatusCode().value()
                            + ". Response: "
                            + responseBody,
                    ex
            );

        } catch (RestClientException ex) {

            log.error(
                    "Unable to communicate with Member Service",
                    ex
            );

            throw new IllegalStateException(
                    "Could not communicate with Member Service. "
                            + "Please try again later.",
                    ex
            );
        }
    }

    /**
     * Deletes a member profile through Member Service.
     */
    public void deleteMemberProfile(Long memberId) {
        try {
            restClient.delete()
                    .uri("/api/members/{id}", memberId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Member profile deletion requested for ID {}",
                    memberId);

        } catch (RestClientResponseException ex) {

            log.warn(
                    "Member Service rejected profile deletion for ID {}. "
                            + "HTTP status: {}, Response: {}",
                    memberId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "Member profile deletion failed. HTTP "
                            + ex.getStatusCode().value()
                            + ". Response: "
                            + ex.getResponseBodyAsString(),
                    ex
            );

        } catch (RestClientException ex) {

            log.error(
                    "Unable to communicate with Member Service "
                            + "while deleting profile ID {}",
                    memberId,
                    ex
            );

            throw new IllegalStateException(
                    "Could not communicate with Member Service "
                            + "while deleting the profile.",
                    ex
            );
        }
    }
}
