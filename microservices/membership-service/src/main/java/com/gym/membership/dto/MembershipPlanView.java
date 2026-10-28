package com.gym.membership.dto;

import java.math.BigDecimal;

public record MembershipPlanView(String name, int durationMonths, BigDecimal price) {
}
