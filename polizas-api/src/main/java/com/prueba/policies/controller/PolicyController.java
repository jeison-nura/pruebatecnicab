package com.prueba.policies.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<PagedResponse<PolicyDTO>> getAllPolicies(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PolicyDTO> policies = policyService.findAll(tipo, estado, pagination(page, size)).map(this::toDTO);
        return ResponseEntity.ok(PagedResponse.from(policies));
    }

    @Override
    @GetMapping("/polizas/{id}/riesgos")
    public ResponseEntity<PagedResponse<RiskDTO>> getRisksByPolicyId(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RiskDTO> risks = policyService.findRisksByPolicyId(id, pagination(page, size)).map(this::toDTO);
        return ResponseEntity.ok(PagedResponse.from(risks));
    }

    private Pageable pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by("createdAt", "id").ascending());
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
