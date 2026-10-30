package com.gym.api_gateway.config;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role"); roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles); return converter;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter, ObjectMapper mapper) throws Exception {
        http.csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/memberships/health","/actuator/health","/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/payments/health").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/payments/webhooks/razorpay").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/payments/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/payments/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET,"/api/members/me").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH,"/api/members/me").hasRole("MEMBER")
                        .requestMatchers("/api/members/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/memberships/me").hasRole("MEMBER")
                        .requestMatchers("/api/memberships/me","/api/memberships/me/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET,"/api/memberships/plans","/api/memberships/plans/**").hasAnyRole("ADMIN","MEMBER")
                        .requestMatchers("/api/memberships/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/trainers/me").hasRole("TRAINER")
                        .requestMatchers(HttpMethod.GET,"/api/trainers/**").hasAnyRole("ADMIN","MEMBER")
                        .requestMatchers("/api/trainers/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req,res,ex)->writeError(req,res,mapper,HttpStatus.UNAUTHORIZED,"Authentication required"))
                        .accessDeniedHandler((req,res,ex)->writeError(req,res,mapper,HttpStatus.FORBIDDEN,"You do not have permission to access this resource")));
        return http.build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper mapper, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value()); response.setContentType("application/json");
        Map<String,Object> body=new LinkedHashMap<>(); body.put("timestamp",Instant.now().toString()); body.put("status",status.value());
        body.put("code",status.name()); body.put("error",status.getReasonPhrase()); body.put("message",message); body.put("path",request.getRequestURI());
        mapper.writeValue(response.getOutputStream(),body);
    }
}
