package com.prueba.policies.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.PolicyStatus;
import com.prueba.policies.domain.Risk;
import com.prueba.policies.domain.RiskStatus;
import com.prueba.policies.repository.IpcRepository;
import com.prueba.policies.repository.PolicyJpaRepository;
import com.prueba.policies.repository.RiskJpaRepository;
import com.prueba.policies.service.exception.BusinessRuleException;
import com.prueba.policies.service.exception.PolicyNotFoundException;
import com.prueba.policies.service.exception.RiskNotFoundException;

/**
 * Business rules of the statement (Spring Boot layer: service).
 * 1. An individual policy can only have 1 risk.
 * 2. A cancelled policy cannot be renewed.
 * 3. Cancelling a policy cancels all its risks (same transaction).
 * 4. Adding a risk requires validating the policy type.
 */
@Service
public class PolicyService {

    private final PolicyJpaRepository policyRepository;
    private final RiskJpaRepository riskRepository;
    private final IpcRepository ipcRepository;

    public PolicyService(PolicyJpaRepository policyRepository, RiskJpaRepository riskRepository, IpcRepository ipcRepository) {
        this.policyRepository = policyRepository;
        this.riskRepository = riskRepository;
        this.ipcRepository = ipcRepository;
    }

    /** Only group policies accept new risks through this endpoint. */
    @Transactional
    public Risk addRisk(UUID policyId, String description) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        if (!policy.getPolicyType().isGroup()) {
            throw new BusinessRuleException("Risks can only be added to group policies");
        }
        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessRuleException("Risks cannot be added to a cancelled policy");
        }

        return riskRepository.save(new Risk(policy, description));
    }

    /**
     * Rule 2: a cancelled policy cannot be renewed.
     * The renewal extends the validity by the initial period, adjusts the monthly
     * rent by the latest applicable IPC, and recalculates the premium.
     */
    @Transactional
    public Policy renew(UUID policyId) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessRuleException("A cancelled policy cannot be renewed");
        }

        long initialMonths = policy.getInitialMonths();
        LocalDate newValidityEnd = policy.getValidityEnd().plusMonths(initialMonths);

        // Adjust monthly rent by the latest IPC applicable at renewal time
        BigDecimal adjustedMonthlyRent = ipcRepository.findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(policy.getValidityEnd())
                .map(ipc -> policy.getMonthlyRent().multiply(
                        BigDecimal.ONE.add(ipc.getPercentage().movePointLeft(2)))
                        .setScale(2, RoundingMode.HALF_UP))
                .orElse(policy.getMonthlyRent());
        policy.setMonthlyRent(adjustedMonthlyRent);

        policy.setValidityEnd(newValidityEnd);
        policy.setStatus(PolicyStatus.RENEWED);
        policy.setPremium(adjustedMonthlyRent.multiply(BigDecimal.valueOf(initialMonths)));

        return policyRepository.save(policy);
    }

    /**
     * Rule 3: cancelling a policy cancels all its risks in the same transaction
     * (they share the Policy aggregate).
     */
    @Transactional
    public Policy cancelPolicy(UUID policyId) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
        policy.setStatus(PolicyStatus.CANCELLED);

        riskRepository.findByPolicyIdAndStatus(policyId, RiskStatus.ACTIVE)
                .forEach(Risk::cancel);

        return policyRepository.save(policy);
    }

    /** Cancels a single risk by id. */
    @Transactional
    public Risk cancelRisk(UUID riskId) {
        Risk risk = riskRepository.findById(riskId)
                .orElseThrow(() -> new RiskNotFoundException(riskId));
        risk.cancel();
        return riskRepository.save(risk);
    }

    public List<Policy> findAll(String tipo, String estado) {
        if (tipo != null && estado != null) {
            return policyRepository.findByPolicyType_NameAndStatus(tipo, PolicyStatus.valueOf(estado.toUpperCase()));
        } else if (tipo != null) {
            return policyRepository.findByPolicyType_Name(tipo);
        } else if (estado != null) {
            return policyRepository.findByStatus(PolicyStatus.valueOf(estado.toUpperCase()));
        }
        return policyRepository.findAll();
    }

    public List<Risk> findRisksByPolicyId(UUID policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new PolicyNotFoundException(policyId);
        }
        return riskRepository.findByPolicyId(policyId);
    }
}
