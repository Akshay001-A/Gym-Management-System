package com.gym.memberservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String,Object>> error(HttpStatus status,String message,HttpServletRequest request){
        Map<String,Object> body=new LinkedHashMap<>();body.put("timestamp",Instant.now().toString());body.put("status",status.value());body.put("code",status.name());body.put("error",status.getReasonPhrase());body.put("message",message);body.put("path",request.getRequestURI());return ResponseEntity.status(status).body(body);
    }
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(MemberNotFoundException ex,HttpServletRequest req){return error(HttpStatus.NOT_FOUND,"Member profile not found",req);}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex,HttpServletRequest req){return error(HttpStatus.BAD_REQUEST,"One or more member fields are invalid",req);}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,Object>> invalidBody(HttpMessageNotReadableException ex,HttpServletRequest req){return error(HttpStatus.BAD_REQUEST,"Invalid JSON request or field type",req);}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> status(ResponseStatusException ex,HttpServletRequest req){HttpStatus status=HttpStatus.valueOf(ex.getStatusCode().value());return error(status,ex.getReason()==null?status.getReasonPhrase():ex.getReason(),req);}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> badRequest(IllegalArgumentException ex,HttpServletRequest req){return error(HttpStatus.BAD_REQUEST,"Invalid member request",req);}
    public ResponseEntity<Map<String,String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String,String> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", String.valueOf(HttpStatus.CONFLICT.value()));
        body.put("code", HttpStatus.CONFLICT.name());
        body.put("error", "The request conflicts with existing database data");
        body.put("message", "The request conflicts with existing database data");
        body.put("path", "/api/members");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> conflict(DataIntegrityViolationException ex,HttpServletRequest req){return error(HttpStatus.CONFLICT,"Member data conflicts with an existing record",req);}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> unexpected(Exception ex,HttpServletRequest req){return error(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected server error occurred",req);}
}