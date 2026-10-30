package com.gym.payment.exception;

public class WebhookRetryableException extends RuntimeException {

    public WebhookRetryableException(String message) {
        super(message);
    }

    public WebhookRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}