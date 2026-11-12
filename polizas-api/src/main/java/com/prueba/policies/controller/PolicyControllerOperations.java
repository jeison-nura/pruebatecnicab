package com.prueba.policies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

public interface PolicyControllerOperations {

    ResponseEntity<List<PolicyDTO>> getAllPolicies(String type, String status);

    ResponseEntity<List<RiskDTO>> getRisksByPolicyId(UUID policyId);

    ResponseEntity<RiskDTO> addRisk(UUID policyId, CreateRiskRequest request);

    ResponseEntity<PolicyDTO> renewPolicy(UUID policyId);

    ResponseEntity<PolicyDTO> cancelPolicy(UUID policyId);

    ResponseEntity<RiskDTO> cancelRisk(UUID riskId);
}

record PolicyDTO(UUID id, String type, String status, String validityStart, String validityEnd,
                String monthlyRent, String premium, String createdAt) {}

record RiskDTO(UUID id, UUID policyId, String description, String status, String createdAt) {}

record CreateRiskRequest(String description) {}