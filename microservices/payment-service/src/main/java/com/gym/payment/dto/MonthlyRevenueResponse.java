package com.gym.payment.dto;

import java.time.YearMonth;

public record MonthlyRevenueResponse(YearMonth month, long amountPaise, long successfulPaymentCount, String currency) { }
