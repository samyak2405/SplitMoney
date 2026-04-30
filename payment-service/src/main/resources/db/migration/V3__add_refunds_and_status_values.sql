-- =============================================================================
-- V3: Add refunds table + expand payments.status column for refund statuses.
-- New statuses: REFUND_INITIATED (16 chars), REFUNDED (8 chars) — both
-- fit within the existing VARCHAR(20) column on payments.
-- =============================================================================

CREATE TABLE refunds (
    id                      UUID            PRIMARY KEY,
    payment_id              UUID            NOT NULL REFERENCES payments(id),
    hyperswitch_refund_id   VARCHAR(120)    UNIQUE,
    amount                  NUMERIC(19,4)   NOT NULL,
    currency                VARCHAR(3)      NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'REFUND_INITIATED',
    reason                  VARCHAR(50)     NOT NULL DEFAULT 'REQUESTED_BY_CUSTOMER',
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL
);

CREATE INDEX ix_refunds_payment_id ON refunds(payment_id);
