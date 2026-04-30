-- =============================================================================
-- V3: SAGA transaction table for payment-integrated settlement flow.
--
-- Each row tracks one full payment SAGA from validation through settlement.
-- Status lifecycle:
--   VALIDATION_PASSED → PAYMENT_INITIATED → PAYMENT_COMPLETED
--     → SETTLEMENT_CONFIRMED   (happy path)
--   VALIDATION_PASSED → PAYMENT_INITIATED → PAYMENT_FAILED
--     → COMPENSATED            (failure path — balance was never changed so no
--                               rollback needed; status is informational)
-- =============================================================================

CREATE TABLE saga_transactions (
    id               UUID         PRIMARY KEY,
    group_id         BIGINT       NOT NULL,
    payer_user_id    UUID         NOT NULL,
    payee_user_id    UUID         NOT NULL,
    amount           NUMERIC(18,2) NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    payment_method   VARCHAR(20)  NOT NULL,
    payment_id       UUID,
    checkout_url     VARCHAR(1024),
    status           VARCHAR(30)  NOT NULL DEFAULT 'VALIDATION_PASSED',
    error_reason     TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_saga_payment_id ON saga_transactions(payment_id) WHERE payment_id IS NOT NULL;
CREATE INDEX ix_saga_status     ON saga_transactions(status);
