-- Version timestamp 1794460185167 from UUIDv7 01a1ce29-724f-7d0f-b5c6-e739d48c2a60.
ALTER TABLE policy.policy ADD COLUMN initial_months BIGINT;
UPDATE policy.policy SET initial_months = GREATEST(1,
    (YEAR(validity_end) - YEAR(validity_start)) * 12 + MONTH(validity_end) - MONTH(validity_start)
    - CASE WHEN DAY(validity_end) < DAY(validity_start) THEN 1 ELSE 0 END);
ALTER TABLE policy.policy ALTER COLUMN initial_months SET NOT NULL;
