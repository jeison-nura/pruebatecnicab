package com.prueba.policies.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.prueba.policies.domain.ApiIdempotency;

public interface ApiIdempotencyRepository extends JpaRepository<ApiIdempotency, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO policy.api_idempotency (idempotency_key, request_hash, request_method, "
            + "request_path, state, response_status, created_at, expires_at) "
            + "VALUES (:key, :hash, :method, :path, 'IN_PROGRESS', NULL, CURRENT_TIMESTAMP, :expiresAt)", nativeQuery = true)
    int claim(String key, String hash, String method, String path, LocalDateTime expiresAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE policy.api_idempotency SET state = 'COMPLETED', response_status = :status, "
            + "response_body = :body WHERE idempotency_key = :key AND request_hash = :hash "
            + "AND state = 'IN_PROGRESS'", nativeQuery = true)
    int complete(String key, String hash, int status, String body);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM policy.api_idempotency WHERE idempotency_key = :key AND request_hash = :hash "
            + "AND state = 'IN_PROGRESS'", nativeQuery = true)
    int release(String key, String hash);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM policy.api_idempotency WHERE idempotency_key = :key AND expires_at < :now "
            + "AND state = 'COMPLETED'", nativeQuery = true)
    int deleteExpiredCompleted(String key, LocalDateTime now);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM policy.api_idempotency WHERE expires_at < :now AND state = 'COMPLETED'", nativeQuery = true)
    int deleteExpired(LocalDateTime now);
}
