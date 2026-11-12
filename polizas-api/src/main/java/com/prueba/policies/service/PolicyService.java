package com.prueba.policies.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prueba.policies.repository.IpcRepository;
import com.prueba.policies.repository.PolicyJpaRepository;
import com.prueba.policies.repository.RiskJpaRepository;
import com.prueba.policies.service.exception.BusinessRuleException;
import com.prueba.policies.service.exception.PolicyNotFoundException;
import com.prueba.policies.service.exception.RiskNotFoundException;
import com.prueba.policies.domain.Ipc;
import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.PolicyStatus;
import com.prueba.policies.domain.Risk;
import com.prueba.policies.domain.RiskStatus;

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

    /**
     * Rule 4: adding a risk requires validating the policy type.
     * Rule 1: an individual policy can only have 1 (active) risk.
     * Group policies accept 1..N risks.
     */
    @Transactional
    public Risk addRisk(UUID policyId, String description) {
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        if (policy.getPolicyType().isIndividual()
                && riskRepository.countByPolicyIdAndStatus(policyId, RiskStatus.ACTIVE) >= 1) {
            throw new BusinessRuleException("An individual policy can only have 1 active risk");
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

        long initialMonths = policy.monthsOfValidity();
        LocalDate newValidityEnd = policy.getValidityEnd().plusMonths(initialMonths);

        // Adjust monthly rent by the latest IPC applicable at renewal time
        BigDecimal adjustedMonthlyRent = policy.getMonthlyRent();
        Optional<Ipc> applicableIpc = ipcRepository.findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(newValidityEnd);
        if (applicableIpc.isPresent()) {
            BigDecimal factor = BigDecimal.ONE.add(applicableIpc.get().getPercentage().divide(BigDecimal.valueOf(100)));
            adjustedMonthlyRent = policy.getMonthlyRent().multiply(factor).setScale(2, BigDecimal.ROUND_HALF_UP);
            policy.setMonthlyRent(adjustedMonthlyRent);
        }

        policy.setValidityEnd(newValidityEnd);
        policy.setStatus(PolicyStatus.RENEWED);
        long totalMonths = ChronoUnit.MONTHS.between(policy.getValidityStart(), newValidityEnd);
        policy.setPremium(adjustedMonthlyRent.multiply(BigDecimal.valueOf(totalMonths)));

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

    private Policy findPolicy(UUID policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
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
        return riskRepository.findByPolicyId(policyId);
    }
}
