
package com.gym.trainer.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, String>> errorResponse(
            HttpStatus status, String message) {

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(TrainerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            TrainerNotFoundException ex) {

        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateTrainerEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(
            DuplicateTrainerEmailException ex) {

        return errorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request data");

        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON request or data type"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseConflict(
            DataIntegrityViolationException ex) {

        return errorResponse(
                HttpStatus.CONFLICT,
                "A database constraint was violated. Check for duplicate email or missing required data."
        );
    }
}
