
package com.gym.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreatePaymentRequest {

    @NotBlank(message = "Plan name is required")
    @Size(max = 30, message = "Plan name must not exceed 30 characters")
    @Pattern(
            regexp = "MONTHLY|QUARTERLY|YEARLY",
            message = "Plan name must be MONTHLY, QUARTERLY, or YEARLY"
    )
    private String planName;

    public CreatePaymentRequest() {
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}
