-- Policy type catalog (fixed ids: 1 = INDIVIDUAL, 2 = GROUP)
CREATE TABLE policy.policy_type (
    id          INT PRIMARY KEY,
    name        VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO policy.policy_type (id, name, description) VALUES
    (1, 'INDIVIDUAL', 'Holder and insured: lessee; beneficiary: landlord'),
    (2, 'GROUP',      'Insured: lessees; beneficiaries: landlords (real estate agencies and property administrations)');

-- Policy: ONE single table; the type references the catalog (FK).
-- PK = UUIDv7 (non-guessable through the API; generatable on any node).
-- Type-specific rules (1 risk for INDIVIDUAL, N for GROUP) are service-layer logic.
CREATE TABLE policy.policy (
    id             UUID          PRIMARY KEY,
    policy_type_id INT           NOT NULL REFERENCES policy.policy_type (id),
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RENEWED', 'CANCELLED')),
    validity_start DATE          NOT NULL,
    validity_end   DATE          NOT NULL,
    monthly_rent   NUMERIC(12,2) NOT NULL,
    premium        NUMERIC(14,2) NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE policy.risk (
    id          UUID         PRIMARY KEY,
    policy_id   UUID         NOT NULL REFERENCES policy.policy (id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED')),
    description VARCHAR(255) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);
