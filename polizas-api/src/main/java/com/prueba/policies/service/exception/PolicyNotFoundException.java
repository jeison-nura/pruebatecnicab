package com.prueba.policies.service.exception;

import java.util.UUID;

/** The requested policy does not exist. */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(UUID policyId) {
        super("Policy not found: " + policyId);
    }
}
