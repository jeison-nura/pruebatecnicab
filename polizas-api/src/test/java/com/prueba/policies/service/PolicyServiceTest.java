package com.prueba.policies.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.prueba.policies.domain.Ipc;
import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.PolicyStatus;
import com.prueba.policies.domain.PolicyType;
import com.prueba.policies.domain.Risk;
import com.prueba.policies.domain.RiskStatus;
import com.prueba.policies.repository.IpcRepository;
import com.prueba.policies.repository.PolicyJpaRepository;
import com.prueba.policies.repository.RiskJpaRepository;
import com.prueba.policies.service.exception.BusinessRuleException;
import com.prueba.policies.service.exception.PolicyNotFoundException;
import com.prueba.policies.service.exception.RiskNotFoundException;

/**
 * Prove-It tests for the four business rules of the statement:
 * 1. An individual policy can only have 1 risk.
 * 2. A cancelled policy cannot be renewed.
 * 3. Cancelling a policy cancels all its risks.
 * 4. Adding a risk requires validating the policy type.
 */
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID RISK_ID = UUID.randomUUID();

    @Mock
    private PolicyJpaRepository policyRepository;

    @Mock
    private RiskJpaRepository riskRepository;

    @Mock
    private IpcRepository ipcRepository;

    @InjectMocks
    private PolicyService policyService;

    private PolicyType individual;
    private PolicyType group;

    @BeforeEach
    void setUp() {
        individual = new PolicyType(1, PolicyType.INDIVIDUAL, "Individual policy");
        group = new PolicyType(2, PolicyType.GROUP, "Group policy");
    }

    private Policy activePolicy(PolicyType type, LocalDate start, LocalDate end, String monthlyRent) {
        return new Policy(type, start, end, new BigDecimal(monthlyRent));
    }

    private Ipc ipc(LocalDate applicationDate, String percentage) {
        return new Ipc(applicationDate, new BigDecimal(percentage));
    }

    // Only group policies accept new risks.

    @Test
    @DisplayName("addRisk: póliza individual rechaza el alta incluso sin riesgos")
    void addRisk_rejectsIndividualWithoutRisks() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.addRisk(POLICY_ID, "Inmueble carrera 45"))
                .isInstanceOf(BusinessRuleException.class);
        verify(riskRepository, never()).save(any(Risk.class));
    }

    @Test
    @DisplayName("regla 1: individual con 1 riesgo activo → rechaza otro riesgo")
    void addRisk_rejectsSecondRisk_forIndividualPolicy() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.addRisk(POLICY_ID, "Segundo riesgo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("group");

        verify(riskRepository, never()).save(any(Risk.class));
    }

    @Test
    @DisplayName("regla 4: colectiva admite varios riesgos")
    void addRisk_allowsMultipleRisks_forGroupPolicy() {
        Policy policy = activePolicy(group, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "5000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));
        when(riskRepository.save(any(Risk.class))).thenAnswer(inv -> inv.getArgument(0));

        Risk saved = policyService.addRisk(POLICY_ID, "Riesgo torre norte");

        assertThat(saved.getStatus()).isEqualTo(RiskStatus.ACTIVE);
        verify(riskRepository, never()).countByPolicyIdAndStatus(any(), any());
        verify(riskRepository).save(any(Risk.class));
    }

    @Test
    @DisplayName("addRisk: póliza inexistente → PolicyNotFoundException")
    void addRisk_throws_whenPolicyNotFound() {
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.addRisk(POLICY_ID, "Riesgo"))
                .isInstanceOf(PolicyNotFoundException.class);

        verify(riskRepository, never()).save(any(Risk.class));
    }

    // ── Rule 2: a cancelled policy cannot be renewed ──

    @Test
    @DisplayName("regla 2: renovar una póliza CANCELADA → rechaza")
    void renew_throws_whenPolicyIsCancelled() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        policy.setStatus(PolicyStatus.CANCELLED);
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.renew(POLICY_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");

        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    @DisplayName("renew: extiende vigencia por el periodo inicial, estado RENEWED y recalcula prima sin IPC")
    void renew_extendsValidity_andSetsRenewed_withoutIpc() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ipcRepository.findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(any())).thenReturn(Optional.empty());

        Policy renewed = policyService.renew(POLICY_ID);

        assertThat(renewed.getStatus()).isEqualTo(PolicyStatus.RENEWED);
        assertThat(renewed.getValidityEnd()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(renewed.getPremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(renewed.getMonthlyRent()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("renew: aplica IPC al canon y recalcula prima")
    void renew_adjustsMonthlyRentByIpc_andRecalculatesPremium() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));
        // IPC 3.5% applicable at renewal date
        when(ipcRepository.findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(any()))
                .thenReturn(Optional.of(ipc(LocalDate.of(2026, 6, 1), "3.50")));

        Policy renewed = policyService.renew(POLICY_ID);

        assertThat(renewed.getStatus()).isEqualTo(PolicyStatus.RENEWED);
        assertThat(renewed.getValidityEnd()).isEqualTo(LocalDate.of(2027, 1, 1));
        // Monthly rent: 1000 * 1.035 = 1035.00
        assertThat(renewed.getMonthlyRent()).isEqualByComparingTo(new BigDecimal("1035.00"));
        assertThat(renewed.getPremium()).isEqualByComparingTo(new BigDecimal("6210.00"));
    }

    @Test
    @DisplayName("renovaciones sucesivas mantienen la duración inicial y aplican IPC al vencer")
    void renew_twiceUsesOriginalPeriod() {
        Policy policy = activePolicy(group, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ipcRepository.findFirstByApplicationDateLessThanEqualOrderByApplicationDateDesc(any()))
                .thenAnswer(inv -> {
                    LocalDate date = inv.getArgument(0);
                    return date.isBefore(LocalDate.of(2026, 7, 1)) ? Optional.empty()
                            : Optional.of(ipc(LocalDate.of(2026, 7, 1), "10.00"));
                });

        policyService.renew(POLICY_ID);
        Policy renewed = policyService.renew(POLICY_ID);

        assertThat(renewed.getValidityEnd()).isEqualTo(LocalDate.of(2027, 7, 1));
        assertThat(renewed.getMonthlyRent()).isEqualByComparingTo("1210.00");
        assertThat(renewed.getPremium()).isEqualByComparingTo("7260.00");
    }

    // ── Rule 3: cancelling a policy cancels all its risks ──

    @Test
    @DisplayName("regla 3: cancelar póliza cancela todos sus riesgos activos en la misma transacción")
    void cancelPolicy_cancelsAllActiveRisks() {
        Policy policy = activePolicy(group, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "5000.00");
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));
        Risk risk1 = new Risk(policy, "Riesgo 1");
        Risk risk2 = new Risk(policy, "Riesgo 2");
        when(riskRepository.findByPolicyIdAndStatus(POLICY_ID, RiskStatus.ACTIVE))
                .thenReturn(List.of(risk1, risk2));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        Policy cancelled = policyService.cancelPolicy(POLICY_ID);

        assertThat(cancelled.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(risk1.getStatus()).isEqualTo(RiskStatus.CANCELLED);
        assertThat(risk2.getStatus()).isEqualTo(RiskStatus.CANCELLED);
        verify(riskRepository).findByPolicyIdAndStatus(POLICY_ID, RiskStatus.ACTIVE);
    }

    @Test
    @DisplayName("cancelRisk: cancela un riesgo por id")
    void cancelRisk_cancelsSingleRisk() {
        Policy policy = activePolicy(individual, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000.00");
        Risk risk = new Risk(policy, "Riesgo único");
        when(riskRepository.findById(RISK_ID)).thenReturn(Optional.of(risk));
        when(riskRepository.save(any(Risk.class))).thenAnswer(inv -> inv.getArgument(0));

        Risk cancelled = policyService.cancelRisk(RISK_ID);

        assertThat(cancelled.getStatus()).isEqualTo(RiskStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelRisk: riesgo inexistente → RiskNotFoundException")
    void cancelRisk_throws_whenRiskNotFound() {
        when(riskRepository.findById(RISK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.cancelRisk(RISK_ID))
                .isInstanceOf(RiskNotFoundException.class);
    }

    @Test
    void addRisk_rejectsCancelledGroupPolicy() {
        Policy policy = activePolicy(group, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "1000");
        policy.setStatus(PolicyStatus.CANCELLED);
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.addRisk(POLICY_ID, "Apartment"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("cancelled");
        verify(riskRepository, never()).save(any());
    }

    @Test
    void renew_rejectsMissingPolicy() {
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> policyService.renew(POLICY_ID)).isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void cancelPolicy_rejectsMissingPolicy() {
        when(policyRepository.findByIdForUpdate(POLICY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> policyService.cancelPolicy(POLICY_ID)).isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void findAll_usesMatchingRepositoryForEachFilterCombination() {
        Pageable pageable = PageRequest.of(1, 1);
        when(policyRepository.findByPolicyType_NameAndStatus(PolicyType.GROUP, PolicyStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        when(policyRepository.findByPolicyType_Name(PolicyType.GROUP, pageable)).thenReturn(new PageImpl<>(List.of()));
        when(policyRepository.findByStatus(PolicyStatus.ACTIVE, pageable)).thenReturn(new PageImpl<>(List.of()));
        when(policyRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        assertThat(policyService.findAll(PolicyType.GROUP, "active", pageable).getContent()).isEmpty();
        assertThat(policyService.findAll(PolicyType.GROUP, null, pageable).getContent()).isEmpty();
        assertThat(policyService.findAll(null, "active", pageable).getContent()).isEmpty();
        assertThat(policyService.findAll(null, null, pageable).getContent()).isEmpty();
        verify(policyRepository).findByPolicyType_NameAndStatus(PolicyType.GROUP, PolicyStatus.ACTIVE, pageable);
        verify(policyRepository).findByPolicyType_Name(PolicyType.GROUP, pageable);
        verify(policyRepository).findByStatus(PolicyStatus.ACTIVE, pageable);
        verify(policyRepository).findAll(pageable);
    }

    @Test
    void findRisks_requiresExistingPolicy() {
        Pageable pageable = PageRequest.of(0, 20);
        when(policyRepository.existsById(POLICY_ID)).thenReturn(false, true);
        assertThatThrownBy(() -> policyService.findRisksByPolicyId(POLICY_ID, pageable))
                .isInstanceOf(PolicyNotFoundException.class);
        when(riskRepository.findByPolicyId(POLICY_ID, pageable)).thenReturn(new PageImpl<>(List.of()));
        assertThat(policyService.findRisksByPolicyId(POLICY_ID, pageable).getContent()).isEmpty();
        verify(riskRepository).findByPolicyId(POLICY_ID, pageable);
    }
}
