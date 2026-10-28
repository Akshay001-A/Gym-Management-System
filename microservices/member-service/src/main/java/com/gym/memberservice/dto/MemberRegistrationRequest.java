package com.gym.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MemberRegistrationRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) Integer age,
        @NotBlank @Size(max = 20) String mobile,
        @NotBlank @Size(max = 50) String gymTime,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String gender,
        @NotBlank @Size(max = 100) String fatherName,
        @NotNull @PositiveOrZero BigDecimal amount) {
}
