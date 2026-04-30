-- Splitwise PostgreSQL schema (simple version for DBeaver)
-- No transaction blocks, no partitions, no triggers.

CREATE TABLE IF NOT EXISTS expense_group (
    group_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    currency CHAR(3) NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_group_currency_upper CHECK (currency = upper(currency))
);

CREATE TABLE IF NOT EXISTS group_member (
    group_id BIGINT NOT NULL REFERENCES expense_group(group_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_by_user_id UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT chk_member_role CHECK (role IN ('ADMIN', 'MEMBER'))
);

CREATE TABLE IF NOT EXISTS expense (
    expense_id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES expense_group(group_id) ON DELETE CASCADE,
    paid_by_user_id UUID NOT NULL REFERENCES users(id),
    total_amount NUMERIC(18,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    description VARCHAR(280) NOT NULL,
    split_type VARCHAR(16) NOT NULL CHECK (split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE')),
    expense_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    CONSTRAINT chk_expense_amount_positive CHECK (total_amount > 0),
    CONSTRAINT chk_expense_currency_upper CHECK (currency = upper(currency))
);

CREATE TABLE IF NOT EXISTS expense_split (
    expense_id BIGINT NOT NULL REFERENCES expense(expense_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    share_amount NUMERIC(18,2) NOT NULL,
    share_percentage NUMERIC(7,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (expense_id, user_id),
    CONSTRAINT chk_share_amount_non_negative CHECK (share_amount >= 0),
    CONSTRAINT chk_share_percentage_range CHECK (
        share_percentage IS NULL OR (share_percentage >= 0 AND share_percentage <= 100)
    )
);

CREATE TABLE IF NOT EXISTS group_balance (
    group_id BIGINT NOT NULL REFERENCES expense_group(group_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    net_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS idempotency_key (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users(id),
    endpoint VARCHAR(100) NOT NULL,
    idem_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status SMALLINT NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency UNIQUE (actor_user_id, endpoint, idem_key),
    CONSTRAINT chk_idem_expiry CHECK (expires_at > created_at)
);

CREATE INDEX IF NOT EXISTS idx_expense_group_date_desc
    ON expense (group_id, expense_date DESC, expense_id DESC);

CREATE INDEX IF NOT EXISTS idx_expense_group_payer_date_desc
    ON expense (group_id, paid_by_user_id, expense_date DESC);

CREATE INDEX IF NOT EXISTS idx_expense_split_expense
    ON expense_split (expense_id);

CREATE INDEX IF NOT EXISTS idx_group_member_user
    ON group_member (user_id, group_id);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at
    ON idempotency_key (expires_at);
