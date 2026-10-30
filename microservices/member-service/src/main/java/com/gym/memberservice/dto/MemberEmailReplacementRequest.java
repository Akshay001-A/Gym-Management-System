package com.gym.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberEmailReplacementRequest(@NotBlank @Email @Size(max = 150) String email) { }
