package com.gym.membership.dto;

import com.gym.membership.enums.MembershipPlan;
import jakarta.validation.constraints.NotNull;

public record PlanEnrollmentRequest(@NotNull MembershipPlan plan) {
}
