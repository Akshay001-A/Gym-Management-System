
package com.gym.authservice.controller;

import com.gym.authservice.entity.Account;
import com.gym.authservice.service.AdminSetupService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin-setup")
@Validated
public class AdminSetupController {

    private final AdminSetupService adminSetupService;

    public AdminSetupController(AdminSetupService adminSetupService) {
        this.adminSetupService = adminSetupService;
    }

    public record AdminSetupRequest(
            @NotBlank String secret,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 12, max = 72) String password
    ) {}

    @PostMapping("/initial-admin")
    public ResponseEntity<?> createInitialAdmin(
            @jakarta.validation.Valid @RequestBody AdminSetupRequest request) {

        Account admin = adminSetupService.createInitialAdmin(
                request.secret(),
                request.name(),
                request.email(),
                request.password()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Initial admin created successfully",
                        "accountId", admin.getId(),
                        "email", admin.getEmail(),
                        "role", admin.getRole()
                )
        );
    }
}
