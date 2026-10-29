
package com.gym.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyPaymentRequest {

    @NotBlank(message = "Razorpay order ID is required")
    @Size(max = 100, message = "Order ID must not exceed 100 characters")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    @Size(max = 100, message = "Payment ID must not exceed 100 characters")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Size(max = 256, message = "Signature must not exceed 256 characters")
    private String razorpaySignature;

    public VerifyPaymentRequest() {
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
}
