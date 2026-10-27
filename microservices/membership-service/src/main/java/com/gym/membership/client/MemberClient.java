
package com.gym.membership.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MemberClient {

    private final RestClient restClient;

    public MemberClient(
            @Value("${services.member-service.url:http://localhost:8081}")
            String memberServiceUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(memberServiceUrl)
                .build();
    }

    /**
     * Checks whether a member exists in Member Service.
     *
     * @param memberId member profile ID
     * @return true when the member exists
     * @throws IllegalArgumentException when the ID is invalid
     * @throws MemberNotFoundException when the member does not exist
     * @throws MemberServiceUnavailableException when Member Service
     *         cannot complete the request
     */
    public boolean memberExists(Long memberId) {

        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(
                    "A valid member ID is required."
            );
        }

        try {
            restClient.get()
                    .uri("/api/members/{id}", memberId)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (request, response) -> {
                                throw new MemberNotFoundException(memberId);
                            }
                    )
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new MemberServiceUnavailableException(
                                        "Member Service returned HTTP "
                                                + response.getStatusCode()
                                );
                            }
                    )
                    .toBodilessEntity();

            return true;

        } catch (MemberNotFoundException
                 | MemberServiceUnavailableException ex) {
            throw ex;

        } catch (RestClientResponseException ex) {
            throw new MemberServiceUnavailableException(
                    "Member Service returned HTTP "
                            + ex.getStatusCode().value()
            );

        } catch (RestClientException ex) {
            throw new MemberServiceUnavailableException(
                    "Could not communicate with Member Service."
            );
        }
    }

    public static class MemberNotFoundException
            extends RuntimeException {

        public MemberNotFoundException(Long id) {
            super("Member not found with ID: " + id);
        }
    }

    public static class MemberServiceUnavailableException
            extends RuntimeException {

        public MemberServiceUnavailableException(String message) {
            super(message);
        }
    }
}
