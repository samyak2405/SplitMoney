-- =============================================================================
-- Splitewise Backend V2 — settlement history table
-- Records each peer-to-peer settlement within a group.
-- =============================================================================

CREATE TABLE IF NOT EXISTS settlement (
    id              UUID          PRIMARY KEY,
    group_id        BIGINT        NOT NULL REFERENCES expense_group(group_id),
    paid_by_user_id UUID          NOT NULL REFERENCES users(id),
    paid_to_user_id UUID          NOT NULL REFERENCES users(id),
    amount          NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)    NOT NULL,
    notes           TEXT,
    settled_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_settlement_group
    ON settlement (group_id, settled_at DESC);

CREATE INDEX IF NOT EXISTS idx_settlement_paid_by
    ON settlement (paid_by_user_id, group_id);
