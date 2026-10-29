
package com.gym.payment.dto;

public class CreatePaymentResponse {

    private Long paymentId;
    private String razorpayOrderId;
    private Long amountPaise;
    private String currency;
    private String razorpayKeyId;
    private String planName;
    private String status;

    public CreatePaymentResponse() {
    }

    public CreatePaymentResponse(
            Long paymentId,
            String razorpayOrderId,
            Long amountPaise,
            String currency,
            String razorpayKeyId,
            String planName,
            String status
    ) {
        this.paymentId = paymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.razorpayKeyId = razorpayKeyId;
        this.planName = planName;
        this.status = status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
