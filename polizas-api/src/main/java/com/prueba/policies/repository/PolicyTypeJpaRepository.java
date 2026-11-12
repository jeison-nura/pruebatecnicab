package com.prueba.policies.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prueba.policies.domain.PolicyType;

/**
 * Driven adapter for the static catalog. Its port will be declared by the
 * create-policy use case (endpoints slice).
 */
public interface PolicyTypeJpaRepository extends JpaRepository<PolicyType, Integer> {

    PolicyType findByName(String name);
}
