-- API Idempotency: cache de respuestas para claves de idempotencia del cliente.
-- Solo endpoints mutantes (POST) la usan; GET son idempotentes por HTTP.
CREATE TABLE policy.api_idempotency (
    idempotency_key VARCHAR(64) PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,
    request_method  VARCHAR(10)  NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    response_status INT          NOT NULL,
    response_body   CLOB,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_api_idempotency_expires ON policy.api_idempotency (expires_at);