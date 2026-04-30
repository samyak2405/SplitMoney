-- =============================================================================
-- Splitewise Backend — initial schema (splitmoney)
-- Managed by Flyway. Do not hand-edit applied migrations.
-- Table definitions match entity @Column annotations exactly.
-- Cross-service user IDs are plain UUID columns (no cross-schema FK).
-- =============================================================================

-- ── users (read-model) ───────────────────────────────────────────────────────
-- Denormalised copy of auth users. Populated by the auth service writing
-- directly on registration (current shared-DB approach) or via events once
-- user-sync events are implemented.
-- NOTE: phone maps to Splitwise User.phoneNumber (@Column name="phone").
-- TODO: remove auth-internal fields (password_hash etc.) when user-sync
--       events replace the shared-write pattern.
CREATE TABLE IF NOT EXISTS users (
    id                      UUID         PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    phone                   VARCHAR(255) NOT NULL UNIQUE,
    currency                VARCHAR(255),
    name                    VARCHAR(255),
    password_hash           VARCHAR(255) NOT NULL,
    password_algo           VARCHAR(32)  NOT NULL,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    account_expires_at      TIMESTAMPTZ,
    locked_until            TIMESTAMPTZ,
    lock_reason             VARCHAR(255),
    is_mfa_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_method              VARCHAR(32),
    authentication_method   VARCHAR(32),
    failed_login_count      INT          NOT NULL DEFAULT 0,
    last_failed_login_at    TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    password_changed_at     TIMESTAMPTZ,
    password_expires_at     TIMESTAMPTZ,
    must_change_password    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL
);

-- ── expense_group ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expense_group (
    group_id            BIGSERIAL    PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    description         VARCHAR(500),
    currency            VARCHAR(3)   NOT NULL CHECK (currency = UPPER(currency)),
    created_by_user_id  UUID         NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL
);

-- ── group_member ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS group_member (
    group_id            BIGINT       NOT NULL REFERENCES expense_group(group_id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES users(id),
    role                VARCHAR(16)  NOT NULL,
    joined_at           TIMESTAMPTZ  NOT NULL,
    added_by_user_id    UUID         NOT NULL REFERENCES users(id),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_group_member_user
    ON group_member (user_id, group_id);

-- ── expense ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expense (
    expense_id          BIGSERIAL    PRIMARY KEY,
    group_id            BIGINT       NOT NULL REFERENCES expense_group(group_id),
    paid_by_user_id     UUID         NOT NULL REFERENCES users(id),
    total_amount        NUMERIC(18,2) NOT NULL CHECK (total_amount > 0),
    currency            VARCHAR(3)   NOT NULL,
    description         VARCHAR(280) NOT NULL,
    split_type          VARCHAR(16)  NOT NULL,
    expense_date        TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by_user_id  UUID         NOT NULL REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_expense_group_date_desc
    ON expense (group_id, expense_date, expense_id);

CREATE INDEX IF NOT EXISTS idx_expense_group_payer_date_desc
    ON expense (group_id, paid_by_user_id, expense_date);

-- ── expense_split ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expense_split (
    expense_id          BIGINT        NOT NULL REFERENCES expense(expense_id) ON DELETE CASCADE,
    user_id             UUID          NOT NULL REFERENCES users(id),
    share_amount        NUMERIC(18,2) NOT NULL CHECK (share_amount >= 0),
    share_percentage    NUMERIC(7,4)  CHECK (share_percentage >= 0 AND share_percentage <= 100),
    created_at          TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (expense_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_expense_split_expense
    ON expense_split (expense_id);

-- ── group_balance ─────────────────────────────────────────────────────────────
-- net_balance > 0 means user is owed money; < 0 means user owes money.
CREATE TABLE IF NOT EXISTS group_balance (
    group_id    BIGINT        NOT NULL REFERENCES expense_group(group_id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL REFERENCES users(id),
    net_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

-- ── idempotency_key ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS idempotency_key (
    id              BIGSERIAL    PRIMARY KEY,
    actor_user_id   UUID         NOT NULL,
    endpoint        VARCHAR(100) NOT NULL,
    idem_key        VARCHAR(128) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status SMALLINT     NOT NULL,
    response_body   JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_idempotency UNIQUE (actor_user_id, endpoint, idem_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at
    ON idempotency_key (expires_at);

-- ── notification_outbox ───────────────────────────────────────────────────────
-- Transactional outbox: events written here in same DB transaction as the
-- business operation, then relayed to RabbitMQ by OutboxPublisherService.
CREATE TABLE IF NOT EXISTS notification_outbox (
    id          UUID         PRIMARY KEY,
    event_id    UUID         NOT NULL UNIQUE,
    event_type  VARCHAR(120) NOT NULL,
    routing_key VARCHAR(120) NOT NULL,
    payload_json JSONB       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_unpublished
    ON notification_outbox (published_at, created_at);
