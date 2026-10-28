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
    private final String internalServiceKey;
    public MemberClient(RestClient.Builder builder, @Value("${services.member-service.url:http://localhost:8081}") String memberServiceUrl,
            @Value("${app.internal-service.key:}") String internalServiceKey) {
        this.restClient = builder.baseUrl(memberServiceUrl).build();
        this.internalServiceKey = internalServiceKey;
    }
    public boolean memberExists(Long memberId) {
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("A valid member ID is required.");
        try {
            Boolean exists = restClient.get().uri("/api/members/internal/{id}/exists", memberId)
                    .header("X-Internal-Service-Key", internalServiceKey).retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> { throw new MemberNotFoundException(memberId); })
                    .onStatus(HttpStatusCode::isError, (request, response) -> { throw new MemberServiceUnavailableException("Member Service returned HTTP " + response.getStatusCode().value()); })
                    .body(Boolean.class);
            if (!Boolean.TRUE.equals(exists)) throw new MemberNotFoundException(memberId);
            return true;
        } catch (MemberNotFoundException | MemberServiceUnavailableException ex) { throw ex; }
        catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new MemberNotFoundException(memberId);
            throw new MemberServiceUnavailableException("Member Service returned HTTP " + ex.getStatusCode().value());
        } catch (RestClientException ex) { throw new MemberServiceUnavailableException("Could not communicate with Member Service."); }
    }
    public static class MemberNotFoundException extends RuntimeException {
        public MemberNotFoundException(Long id) { super("Member not found with ID: " + id); }
    }
    public static class MemberServiceUnavailableException extends RuntimeException {
        public MemberServiceUnavailableException(String message) { super(message); }
    }
}