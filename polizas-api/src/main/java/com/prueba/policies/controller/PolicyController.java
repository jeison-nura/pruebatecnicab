package com.prueba.policies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.Risk;
import com.prueba.policies.service.PolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1")
public class PolicyController implements PolicyControllerOperations {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    @GetMapping("/polizas")
    public ResponseEntity<List<PolicyDTO>> getAllPolicies(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado) {
        List<Policy> policies = policyService.findAll(tipo, estado);
        List<PolicyDTO> dtos = policies.stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    @GetMapping("/polizas/{id}/riesgos")
    public ResponseEntity<List<RiskDTO>> getRisksByPolicyId(@PathVariable UUID id) {
        List<Risk> risks = policyService.findRisksByPolicyId(id);
        List<RiskDTO> dtos = risks.stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    @PostMapping("/polizas/{id}/riesgos")
    public ResponseEntity<RiskDTO> addRisk(@PathVariable UUID id, @Valid @RequestBody CreateRiskRequest request) {
        Risk risk = policyService.addRisk(id, request.description());
        return ResponseEntity.ok(toDTO(risk));
    }

    @Override
    @PostMapping("/polizas/{id}/renovar")
    public ResponseEntity<PolicyDTO> renewPolicy(@PathVariable UUID id) {
        Policy policy = policyService.renew(id);
        return ResponseEntity.ok(toDTO(policy));
    }

    @Override
    @PostMapping("/polizas/{id}/cancelar")
    public ResponseEntity<PolicyDTO> cancelPolicy(@PathVariable UUID id) {
        Policy policy = policyService.cancelPolicy(id);
        return ResponseEntity.ok(toDTO(policy));
    }

    @PostMapping("/riesgos/{id}/cancelar")
    public ResponseEntity<RiskDTO> cancelRisk(@PathVariable UUID id) {
        Risk risk = policyService.cancelRisk(id);
        return ResponseEntity.ok(toDTO(risk));
    }

    private PolicyDTO toDTO(Policy p) {
        return new PolicyDTO(
                p.getId(),
                p.getPolicyType().getName(),
                p.getStatus().name(),
                p.getValidityStart().toString(),
                p.getValidityEnd().toString(),
                p.getMonthlyRent().toPlainString(),
                p.getPremium().toPlainString(),
                p.getCreatedAt().toString()
        );
    }

    private RiskDTO toDTO(Risk r) {
        return new RiskDTO(
                r.getId(),
                r.getPolicy().getId(),
                r.getDescription(),
                r.getStatus().name(),
                r.getCreatedAt().toString()
        );
    }
}
