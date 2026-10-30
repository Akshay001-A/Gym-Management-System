package com.gym.payment.dto;

import java.time.Instant;

public record RevenuePaymentItem(Long paymentId, Long memberId, String memberName, String memberEmail,
        String planName, long amountPaise, String currency, Instant paidAt) { }
