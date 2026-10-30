package com.gym.payment.dto;

import java.util.List;

public record RevenueReportResponse(String period, int year, Integer month, long totalAmountPaise,
        long successfulPaymentCount, String currency, int page, int size, int totalPages,
        List<RevenuePaymentItem> payments) { }
