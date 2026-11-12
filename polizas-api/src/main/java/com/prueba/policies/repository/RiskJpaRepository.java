package com.prueba.policies.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prueba.policies.domain.Risk;
import com.prueba.policies.domain.RiskStatus;

/**
 * Spring Data repository for Risk.
 */
public interface RiskJpaRepository extends JpaRepository<Risk, UUID> {

    List<Risk> findByPolicyIdAndStatus(UUID policyId, RiskStatus status);

    long countByPolicyIdAndStatus(UUID policyId, RiskStatus status);

    List<Risk> findByPolicyId(UUID policyId);

    Page<Risk> findByPolicyId(UUID policyId, Pageable pageable);
}
