package com.gym.authservice.dto;

public record RegistrationCodeResponse(String maskedEmail, String message) {
}
