-- Demo-only Flyway migration; UUIDv7 01a1ce49-8597-7450-9ccb-0a14d9bf9202.
-- The catalogue (policy_type) comes from the regular migrations.
INSERT INTO policy.policy (id, policy_type_id, status, validity_start, validity_end,
                           initial_months, monthly_rent, premium, version)
VALUES
    (CAST('01a1ce48-d512-720a-97c8-386d6a328fdf' AS UUID), 2, 'ACTIVE',
     DATE '2026-07-01', DATE '2027-01-01', 6, 1200.00, 7200.00, 0),
    (CAST('01a1ce48-d515-7548-93ed-b7daeed7900f' AS UUID), 1, 'ACTIVE',
     DATE '2026-07-01', DATE '2027-01-01', 6, 1000.00, 6000.00, 0);

INSERT INTO policy.risk (id, policy_id, status, description, version)
VALUES
    (CAST('01a1ce48-d516-701d-96ec-9373f72cddf2' AS UUID),
     CAST('01a1ce48-d512-720a-97c8-386d6a328fdf' AS UUID), 'ACTIVE', 'North apartment', 0),
    (CAST('01a1ce48-d517-7089-b887-f1a9da727575' AS UUID),
     CAST('01a1ce48-d512-720a-97c8-386d6a328fdf' AS UUID), 'ACTIVE', 'South apartment', 0),
    (CAST('01a1ce48-d518-7fbf-8c37-913c2177a53e' AS UUID),
     CAST('01a1ce48-d515-7548-93ed-b7daeed7900f' AS UUID), 'ACTIVE', 'Individual apartment', 0);

-- Applicable to the sample policies when they expire on 2027-01-01.
INSERT INTO policy.ipc (application_date, percentage) VALUES (DATE '2026-11-01', 3.5000);
