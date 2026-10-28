package com.gym.membership.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentSimulationRequest(@NotNull Outcome outcome) {
    public enum Outcome { SUCCESS, FAILURE }
}
