package com.gym.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteMemberRegistrationRequest(
        @NotBlank @Size(max = 150) String identifier,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Enter the 6-digit code sent to your email") String code,
        @NotBlank @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters") String password) {
}
