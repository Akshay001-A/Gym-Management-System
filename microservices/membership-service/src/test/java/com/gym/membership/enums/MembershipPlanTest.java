
package com.gym.membership.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MembershipPlanTest {

    @Test
    void monthlyPlanShouldHaveCorrectDurationAndPrice() {
        assertEquals(1, MembershipPlan.MONTHLY.getDurationMonths());
        assertEquals("1500.00",
                MembershipPlan.MONTHLY.getPrice().toPlainString());
    }

    @Test
    void quarterlyPlanShouldHaveCorrectDurationAndPrice() {
        assertEquals(3, MembershipPlan.QUARTERLY.getDurationMonths());
        assertEquals("4000.00",
                MembershipPlan.QUARTERLY.getPrice().toPlainString());
    }

    @Test
    void yearlyPlanShouldHaveCorrectDurationAndPrice() {
        assertEquals(12, MembershipPlan.YEARLY.getDurationMonths());
        assertEquals("12000.00",
                MembershipPlan.YEARLY.getPrice().toPlainString());
    }
}
