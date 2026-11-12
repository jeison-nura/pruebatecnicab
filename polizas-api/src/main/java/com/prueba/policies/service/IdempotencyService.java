package com.prueba.policies.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.prueba.policies.domain.ApiIdempotency;
import com.prueba.policies.repository.ApiIdempotencyRepository;

@Service
public class IdempotencyService {

    private static final int RETENTION_HOURS = 24;
    private final ApiIdempotencyRepository repository;

    public IdempotencyService(ApiIdempotencyRepository repository) {
        this.repository = repository;
    }

    public Claim claim(String key, String hash, String method, String path) {
        // Only completed records can expire. An unknown in-flight outcome needs reconciliation.
        repository.deleteExpiredCompleted(key, LocalDateTime.now());
        try {
            repository.claim(key, hash, method, path, LocalDateTime.now().plusHours(RETENTION_HOURS));
            return new Claim(Outcome.CLAIMED, null);
        } catch (DataIntegrityViolationException duplicate) {
            ApiIdempotency existing = repository.findById(key).orElseThrow(() -> duplicate);
            if (!existing.getRequestHash().equals(hash)) {
                return new Claim(Outcome.CONFLICT, null);
            }
            return existing.isCompleted()
                    ? new Claim(Outcome.REPLAY, existing)
                    : new Claim(Outcome.IN_PROGRESS, null);
        }
    }

    public void complete(String key, String hash, int status, String body) {
        if (repository.complete(key, hash, status, body) != 1) {
            throw new IllegalStateException("Idempotency claim was lost");
        }
    }

    public void release(String key, String hash) {
        repository.release(key, hash);
    }

    public enum Outcome { CLAIMED, CONFLICT, IN_PROGRESS, REPLAY }

    public record Claim(Outcome outcome, ApiIdempotency response) {}
}
