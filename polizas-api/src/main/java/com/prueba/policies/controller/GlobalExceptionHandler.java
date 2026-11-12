package com.prueba.policies.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.prueba.policies.service.exception.BusinessRuleException;
import com.prueba.policies.service.exception.PolicyNotFoundException;
import com.prueba.policies.service.exception.RiskNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFound(PolicyNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RiskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRiskNotFound(RiskNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "RISK_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        // Never log exception messages or payloads: they can contain untrusted data.
        log.atError().addKeyValue("errorCode", "INTERNAL_ERROR")
                .addKeyValue("exceptionType", ex.getClass().getSimpleName()).log("http.request.error");
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String code, String message, String path) {
        if (status.is4xxClientError()) {
            log.atWarn().addKeyValue("errorCode", code).addKeyValue("status", status.value())
                    .log("http.request.error");
        }
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                path
        );
        return ResponseEntity.status(status).body(body);
    }

    record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String code,
            String message,
            String path
    ) {}
}
