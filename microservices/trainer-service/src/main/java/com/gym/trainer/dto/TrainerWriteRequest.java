package com.gym.trainer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrainerWriteRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$") String mobile,
        @NotBlank @Size(max = 100) String specialization,
        @NotNull @Min(0) @Max(60) Integer experienceYears,
        @NotBlank @Size(max = 40) String availability,
        @NotBlank @Size(max = 40) String status) {
}
