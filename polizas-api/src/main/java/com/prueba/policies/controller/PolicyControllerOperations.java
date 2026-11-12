package com.prueba.policies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface PolicyControllerOperations {

    ResponseEntity<PagedResponse<PolicyDTO>> getAllPolicies(String type, String status, int page, int size);

    ResponseEntity<PagedResponse<RiskDTO>> getRisksByPolicyId(UUID policyId, int page, int size);

    ResponseEntity<RiskDTO> addRisk(UUID policyId, CreateRiskRequest request);

    ResponseEntity<PolicyDTO> renewPolicy(UUID policyId);

    ResponseEntity<PolicyDTO> cancelPolicy(UUID policyId);

    ResponseEntity<RiskDTO> cancelRisk(UUID riskId);
}

record PolicyDTO(UUID id, String type, String status, String validityStart, String validityEnd,
                String monthlyRent, String premium, String createdAt) {}

record RiskDTO(UUID id, UUID policyId, String description, String status, String createdAt) {}

record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <T> PagedResponse<T> from(org.springframework.data.domain.Page<T> result) {
        return new PagedResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}

record CreateRiskRequest(@NotBlank @Size(max = 255) String description) {}
