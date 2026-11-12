package com.prueba.policies.service.exception;

import java.util.UUID;

/** The requested risk does not exist. */
public class RiskNotFoundException extends RuntimeException {

    public RiskNotFoundException(UUID riskId) {
        super("Risk not found: " + riskId);
    }
}
