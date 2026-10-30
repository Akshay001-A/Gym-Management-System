
package com.gym.payment.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(
            PaymentNotFoundException exception
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Payment Not Found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException exception
    ) {
        return createProblem(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "The authenticated account is not linked to a member profile."
        );
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ProblemDetail handlePaymentProcessing(
            PaymentProcessingException exception
    ) {
        log.error("Payment processing failed", exception);

        return createProblem(
                HttpStatus.BAD_GATEWAY,
                "Payment Processing Failed",
                "The payment service could not complete the operation."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage(),
                        (first, second) -> first
                ));

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more request fields are invalid."
        );

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                exception.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                "The request body is missing or contains invalid data."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(
            IllegalStateException exception
    ) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Invalid Operation",
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected payment service error", exception);

        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
        );
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail == null ? "Request could not be completed." : detail
        );

        problem.setTitle(title);
        problem.setType(URI.create(
                "https://api.gym-management.local/errors/"
                        + status.value()
        ));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }


    @ExceptionHandler(WebhookRetryableException.class)
    public ProblemDetail handleWebhookRetryable(
            WebhookRetryableException exception
    ) {
        log.warn("Retryable Razorpay webhook error: {}", exception.getMessage());

        return createProblem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Webhook Processing Temporarily Unavailable",
                "The webhook could not be processed yet. Please retry."
        );
    }
}
