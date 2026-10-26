
package com.gym.membership.enums;

import java.math.BigDecimal;

public enum MembershipPlan {

    MONTHLY(1, new BigDecimal("1500.00")),
    QUARTERLY(3, new BigDecimal("4000.00")),
    YEARLY(12, new BigDecimal("12000.00"));

    private final int durationMonths;
    private final BigDecimal price;

    MembershipPlan(int durationMonths, BigDecimal price) {
        this.durationMonths = durationMonths;
        this.price = price;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
