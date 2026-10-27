
package com.gym.authservice.config;

import com.gym.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) {

        return token -> {
            try {
                Claims claims = jwtService.extractClaims(token);

                Instant issuedAt = claims.getIssuedAt().toInstant();
                Instant expiresAt = claims.getExpiration().toInstant();

                return new Jwt(
                        token,
                        issuedAt,
                        expiresAt,
                        Map.of(
                                "alg", "HS256"
                        ),
                        Map.of(
                                "sub", claims.getSubject(),
                                "accountId", claims.get("accountId"),
                                "name", claims.get("name"),
                                "role", claims.get("role"),
                                "iat", issuedAt.getEpochSecond(),
                                "exp", expiresAt.getEpochSecond()
                        )
                );

            } catch (Exception ex) {
                throw new JwtException("Invalid or expired JWT", ex);
            }
        };
    }
}
