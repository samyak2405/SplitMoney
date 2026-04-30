-- =============================================================================
-- Auth Backend — initial schema (authdb)
-- Managed by Flyway. Do not hand-edit applied migrations.
-- Table definitions match entity @Column annotations exactly so
-- ddl-auto: validate passes on startup.
-- =============================================================================

-- ── security_policies ────────────────────────────────────────────────────────
-- Must exist before users (users.security_policy_id FK).
CREATE TABLE IF NOT EXISTS security_policies (
    id                          UUID            PRIMARY KEY,
    name                        VARCHAR(64)     NOT NULL UNIQUE,
    password_min_length         INT             NOT NULL,
    password_max_age_days       INT,
    password_history_count      INT             NOT NULL,
    lockout_threshold           INT             NOT NULL,
    lockout_duration_minutes    INT             NOT NULL,
    mfa_required                BOOLEAN         NOT NULL DEFAULT FALSE,
    password_expiry_warning_days INT            NOT NULL,
    password_expiry_days        INT             NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL
);

-- ── users ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                      UUID            PRIMARY KEY,
    email                   VARCHAR(255)    UNIQUE,
    mobile                  VARCHAR(20)     UNIQUE,
    password_hash           VARCHAR(255)    NOT NULL,
    password_algo           VARCHAR(32)     NOT NULL,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    account_expires_at      TIMESTAMPTZ,
    locked_until            TIMESTAMPTZ,
    lock_reason             VARCHAR(255),
    is_mfa_enabled          BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_method              VARCHAR(32),
    authentication_method   VARCHAR(32),
    failed_login_count      INT             NOT NULL DEFAULT 0,
    last_failed_login_at    TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    password_changed_at     TIMESTAMPTZ,
    password_expires_at     TIMESTAMPTZ,
    must_change_password    BOOLEAN         NOT NULL DEFAULT FALSE,
    security_policy_id      UUID            REFERENCES security_policies(id),
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL
);

-- ── roles ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- ── user_roles ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id     UUID        NOT NULL REFERENCES users(id),
    role_id     UUID        NOT NULL REFERENCES roles(id),
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by UUID        REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

-- ── devices ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS devices (
    id                  UUID        PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users(id),
    device_name         VARCHAR(128),
    device_fingerprint  VARCHAR(255) UNIQUE,
    last_seen_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL
);

-- ── refresh_tokens ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users(id),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    issued_at       TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by_id  UUID        REFERENCES refresh_tokens(id),
    device_id       UUID        REFERENCES devices(id),
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(255)
);

-- ── otp_tokens ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS otp_tokens (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL REFERENCES users(id),
    token_hash       VARCHAR(255) NOT NULL,
    purpose          VARCHAR(32)  NOT NULL,
    issued_at        TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL,
    consumed_at      TIMESTAMPTZ,
    attempt_count    INT          NOT NULL DEFAULT 0,
    ip_address       VARCHAR(64),
    delivery_channel VARCHAR(32)
);

CREATE INDEX IF NOT EXISTS idx_otp_user_purpose_consumed_issued
    ON otp_tokens (user_id, purpose, consumed_at, issued_at);

CREATE INDEX IF NOT EXISTS idx_otp_expires_at
    ON otp_tokens (expires_at);

-- ── oauth_identities ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS oauth_identities (
    id                  UUID        PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users(id),
    provider            VARCHAR(32)  NOT NULL,
    provider_subject    VARCHAR(255) NOT NULL,
    email               VARCHAR(255),
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    display_name        VARCHAR(255),
    picture_url         VARCHAR(1024),
    profile_synced_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_oauth_provider_subject UNIQUE (provider, provider_subject)
);

-- ── password_history ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_history (
    id            UUID        PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id),
    password_hash VARCHAR(255) NOT NULL,
    password_algo VARCHAR(32)  NOT NULL,
    changed_at    TIMESTAMPTZ  NOT NULL
);

-- ── auth_parameters ───────────────────────────────────────────────────────────
-- id is BIGSERIAL because entity uses GenerationType.IDENTITY with Long
CREATE TABLE IF NOT EXISTS auth_parameters (
    id          BIGSERIAL   PRIMARY KEY,
    param_id    VARCHAR(255),
    param_value VARCHAR(255),
    is_enabled  BOOLEAN     NOT NULL DEFAULT FALSE
);

-- =============================================================================
-- Seed data
-- =============================================================================

INSERT INTO security_policies (
    id, name,
    password_min_length, password_max_age_days, password_history_count,
    lockout_threshold, lockout_duration_minutes, mfa_required,
    password_expiry_warning_days, password_expiry_days,
    created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000010', 'DEFAULT',
    8, NULL, 5,
    5, 15, FALSE,
    7, 90,
    NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN',  'Administrator', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', 'MEMBER', 'Regular member', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
