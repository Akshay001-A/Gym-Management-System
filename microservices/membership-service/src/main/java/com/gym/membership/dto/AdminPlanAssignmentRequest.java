package com.gym.membership.dto;

import com.gym.membership.enums.MembershipPlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminPlanAssignmentRequest(@NotNull @Positive Long memberId, @NotNull MembershipPlan plan) { }
