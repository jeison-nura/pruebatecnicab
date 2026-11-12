-- Version timestamp 1794460801252 from UUIDv7 01a1ce32-d8e4-70d8-ad41-37ef9385ca38.
-- Existing cached responses are completed; new claims start in progress.
ALTER TABLE policy.api_idempotency ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
    CHECK (state IN ('IN_PROGRESS', 'COMPLETED'));
ALTER TABLE policy.api_idempotency ALTER COLUMN response_status DROP NOT NULL;
