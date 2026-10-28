package com.gym.authservice.config;

import com.gym.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
public class JwtConfig {
    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) {
        return token -> {
            try {
                Claims claims = jwtService.extractClaims(token);
                Instant issuedAt = claims.getIssuedAt().toInstant();
                Instant expiresAt = claims.getExpiration().toInstant();
                Map<String, Object> headers = Map.of("alg", "HS256");
                Map<String, Object> tokenClaims = new HashMap<>();
                tokenClaims.put("sub", claims.getSubject());
                tokenClaims.put("accountId", claims.get("accountId"));
                tokenClaims.put("name", claims.get("name"));
                tokenClaims.put("role", claims.get("role"));
                tokenClaims.put("iat", issuedAt.getEpochSecond());
                tokenClaims.put("exp", expiresAt.getEpochSecond());
                Object memberId = claims.get("memberId");
                if (memberId != null) tokenClaims.put("memberId", memberId);
                return new Jwt(token, issuedAt, expiresAt, headers, tokenClaims);
            } catch (Exception ex) {
                throw new JwtException("Invalid or expired JWT", ex);
            }
        };
    }
}