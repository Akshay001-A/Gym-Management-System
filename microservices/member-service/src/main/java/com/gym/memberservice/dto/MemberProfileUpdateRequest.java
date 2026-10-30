package com.gym.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
        @Size(max = 255) String name,
        @Min(1) Integer age,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$") String mobile,
        @Size(max = 50) String gymTime,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String gender,
        @Size(max = 100) String fatherName) {
    public boolean isEmpty() {
        return name == null && age == null && mobile == null
                && gender == null && fatherName == null;
    }
}
