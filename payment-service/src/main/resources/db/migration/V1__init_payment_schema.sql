-- =============================================================================
-- Payment Service — initial schema (paymentdb)
-- Managed by Flyway. Schema name set via flyway.default-schema in
-- application.yaml — all table names here are unqualified.
-- =============================================================================

-- ── payments ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id                      UUID            PRIMARY KEY,
    payer_user_id           UUID            NOT NULL,
    payee_user_id           UUID            NOT NULL,
    amount                  NUMERIC(19,4)   NOT NULL CHECK (amount >= 1),
    currency                VARCHAR(3)      NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    payment_method          VARCHAR(20)     NOT NULL,
    client_request_id       VARCHAR(120)    NOT NULL,
    hyperswitch_payment_id  VARCHAR(120),
    return_url              VARCHAR(1024),
    checkout_url            VARCHAR(1024),
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    version                 BIGINT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_client_request_id
    ON payments (client_request_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_hyperswitch_payment_id
    ON payments (hyperswitch_payment_id)
    WHERE hyperswitch_payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_payments_status_updated_at
    ON payments (status, updated_at);

-- ── payment_state_history ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_state_history (
    id          UUID        PRIMARY KEY,
    payment_id  UUID        NOT NULL REFERENCES payments(id),
    from_status VARCHAR(20) NOT NULL,
    to_status   VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL,
    reason      VARCHAR(120) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_state_history_payment_id
    ON payment_state_history (payment_id, changed_at DESC);

-- ── idempotency_records ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS idempotency_records (
    id               UUID        PRIMARY KEY,
    idempotency_key  VARCHAR(128) NOT NULL,
    endpoint         VARCHAR(100) NOT NULL,
    request_hash     VARCHAR(128) NOT NULL,
    payment_id       UUID         NOT NULL REFERENCES payments(id),
    response_body    TEXT         NOT NULL,
    response_status  INTEGER      NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_idempotency_key_endpoint UNIQUE (idempotency_key, endpoint)
);

-- ── outbox_events ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID        PRIMARY KEY,
    aggregate_type  VARCHAR(80)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(120) NOT NULL,
    payload         TEXT         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_outbox_unpublished
    ON outbox_events (published, created_at);

-- ── processed_webhooks ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS processed_webhooks (
    id          UUID        PRIMARY KEY,
    webhook_id  VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_processed_webhooks_webhook_id UNIQUE (webhook_id)
);
