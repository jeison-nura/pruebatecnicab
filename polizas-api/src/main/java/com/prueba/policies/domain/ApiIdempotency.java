package com.prueba.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/** Cache de respuesta para idempotencia de API (cliente → servidor). */
@Entity
@Table(name = "api_idempotency", schema = "policy")
public class ApiIdempotency {

    @Id
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "request_method", length = 10, nullable = false)
    private String requestMethod;

    @Column(name = "request_path", length = 255, nullable = false)
    private String requestPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "CLOB")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected ApiIdempotency() {
        // JPA
    }

    public enum State { IN_PROGRESS, COMPLETED }

    public ApiIdempotency(String idempotencyKey, String requestHash, String requestMethod,
                          String requestPath, int responseStatus, String responseBody,
                          LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
        this.state = State.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getRequestMethod() { return requestMethod; }
    public String getRequestPath() { return requestPath; }
    public int getResponseStatus() { return responseStatus; }
    public boolean isCompleted() { return state == State.COMPLETED; }
    public String getResponseBody() { return responseBody; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
