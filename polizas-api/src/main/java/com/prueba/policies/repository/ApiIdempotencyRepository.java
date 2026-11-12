package com.prueba.policies.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.prueba.policies.domain.ApiIdempotency;

public interface ApiIdempotencyRepository extends JpaRepository<ApiIdempotency, String> {

    Optional<ApiIdempotency> findByIdempotencyKeyAndRequestHash(String key, String hash);

    @Modifying
    @Transactional
    @Query("DELETE FROM ApiIdempotency a WHERE a.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}