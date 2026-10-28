package com.gym.memberservice.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceKeyFilter extends OncePerRequestFilter {
    private final String serviceKey;
    private final ObjectMapper objectMapper;
    public InternalServiceKeyFilter(@Value("${app.internal-service.key:}") String serviceKey, ObjectMapper objectMapper) {
        this.serviceKey = serviceKey;
        this.objectMapper = objectMapper;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.equals("/api/members/internal") || path.startsWith("/api/members/internal/"));
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader("X-Internal-Service-Key");
        boolean valid = serviceKey != null && !serviceKey.isBlank() && supplied != null
                && MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("code", "UNAUTHORIZED");
            body.put("error", "Unauthorized");
            body.put("message", "Internal service authentication required");
            body.put("path", request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken("internal-service", null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}