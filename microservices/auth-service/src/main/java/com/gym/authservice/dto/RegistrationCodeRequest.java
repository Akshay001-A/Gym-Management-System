package com.gym.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationCodeRequest(
        @NotBlank @Size(max = 150) String identifier) {
}
