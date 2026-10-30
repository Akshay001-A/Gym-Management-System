package com.gym.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeCodeRequest(@NotBlank @Email @Size(max = 150) String newEmail) { }
