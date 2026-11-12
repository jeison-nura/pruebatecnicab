package com.prueba.policies.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.prueba.policies.domain.Policy;
import com.prueba.policies.domain.PolicyStatus;

/** Spring Data repository for Policy. */
public interface PolicyJpaRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByPolicyType_Name(String name);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByPolicyType_NameAndStatus(String name, PolicyStatus status);

    /**
     * Pessimistic lock (SELECT FOR UPDATE) — serializes concurrent access
     * to the same policy row until the transaction commits/rollbacks.
     * Prevents race conditions in renew/cancel/addRisk.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Policy p WHERE p.id = :id")
    Optional<Policy> findByIdForUpdate(UUID id);
}