
package com.gym.membership.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MemberClient {

    private final RestClient restClient;

    public MemberClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    public boolean memberExists(Long memberId) {
        try {
            restClient.get()
                    .uri("/api/members/{id}", memberId)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new MemberNotFoundException(memberId);
                            }
                    )
                    .toBodilessEntity();

            return true;

        } catch (MemberNotFoundException ex) {
            throw ex;
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
