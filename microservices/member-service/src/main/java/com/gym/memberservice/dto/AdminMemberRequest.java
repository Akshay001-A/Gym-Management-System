package com.gym.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminMemberRequest(
        @jakarta.validation.constraints.NotBlank @Size(max = 255) String name,
        @Min(1) Integer age,
        String mobile,
        String gymTime,
        @Email @Size(max = 150) String email,
        String gender,
        String fatherName,
        @PositiveOrZero BigDecimal amount) {
}
