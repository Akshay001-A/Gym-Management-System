
package com.gym.payment.exception;

public class PaymentProcessingException extends RuntimeException {

    private final boolean outcomeUnknown;

    public PaymentProcessingException(String message) {
        super(message);
        this.outcomeUnknown = false;
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.outcomeUnknown = false;
    }

    public PaymentProcessingException(
            String message,
            Throwable cause,
            boolean outcomeUnknown
    ) {
        super(message, cause);
        this.outcomeUnknown = outcomeUnknown;
    }

    public boolean isOutcomeUnknown() {
        return outcomeUnknown;
    }
}
