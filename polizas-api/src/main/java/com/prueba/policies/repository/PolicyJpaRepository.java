package com.prueba.policies.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.PolicyStatus;

/** Spring Data repository for Policy. */
public interface PolicyJpaRepository extends JpaRepository<Policy, UUID> {

    @EntityGraph(attributePaths = "policyType")
    List<Policy> findByPolicyType_Name(String name);

    @EntityGraph(attributePaths = "policyType")
    List<Policy> findByStatus(PolicyStatus status);

    @EntityGraph(attributePaths = "policyType")
    List<Policy> findByPolicyType_NameAndStatus(String name, PolicyStatus status);

    @Override
    @EntityGraph(attributePaths = "policyType")
    List<Policy> findAll();

    /**
     * Pessimistic lock (SELECT FOR UPDATE) — serializes concurrent access
     * to the same policy row until the transaction commits/rollbacks.
     * Prevents race conditions in renew/cancel/addRisk.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Policy p JOIN FETCH p.policyType WHERE p.id = :id")
    Optional<Policy> findByIdForUpdate(UUID id);
}
