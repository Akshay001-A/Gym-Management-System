
package com.gym.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Read the role claim from your JWT.
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter);

        return jwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/actuator/health"
                        ).permitAll()

                        // Members may access their own profile endpoint.
                        // These specific endpoints must exist in your controller.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/me",
                                "/api/members/me/**"
                        ).hasAnyRole("ADMIN", "MEMBER")

                        // Only Admin can access general member management.
                        .requestMatchers("/api/members/**")
                        .hasRole("ADMIN")

                        // Members can view their own membership.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/memberships/me",
                                "/api/memberships/me/**"
                        ).hasAnyRole("ADMIN", "MEMBER")

                        // Only Admin can manage other membership endpoints.
                        .requestMatchers("/api/memberships/**")
                        .hasRole("ADMIN")

                        // Trainers can access their own trainer profile.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/trainers/me",
                                "/api/trainers/me/**"
                        ).hasAnyRole("ADMIN", "TRAINER")

                        // Only Admin can manage general trainer endpoints.
                        .requestMatchers("/api/trainers/**")
                        .hasRole("ADMIN")

                        // All other endpoints require a valid token.
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(
                                    HttpStatus.UNAUTHORIZED.value());
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(
                                    HttpStatus.FORBIDDEN.value());
                        })
                );

        return http.build();
    }
}
