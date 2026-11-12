package com.prueba.policies.service.exception;

/** A business rule of the statement was violated. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
