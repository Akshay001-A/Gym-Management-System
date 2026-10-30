
package com.gym.authservice.controller;

import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.LoginRequest;
import com.gym.authservice.dto.RegistrationCodeRequest;
import com.gym.authservice.dto.CompleteMemberRegistrationRequest;
import com.gym.authservice.dto.RegistrationCodeResponse;
import com.gym.authservice.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/request-code")
    public ResponseEntity<RegistrationCodeResponse> requestRegistrationCode(
            @Valid @RequestBody RegistrationCodeRequest request) {
        return ResponseEntity.ok(authService.requestMemberRegistrationCode(request.identifier()));
    }

    @PostMapping("/register/complete")
    public ResponseEntity<AuthResponse> completeRegistration(
            @Valid @RequestBody CompleteMemberRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                authService.completeMemberRegistration(
                        request.identifier(), request.code(), request.password()));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> legacyRegistration() {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.GONE, "Use email verification to create a member account.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
