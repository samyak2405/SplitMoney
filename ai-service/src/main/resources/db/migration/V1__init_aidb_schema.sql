CREATE SCHEMA IF NOT EXISTS aidb;

CREATE TABLE IF NOT EXISTS aidb.ai_interaction_log (
    id               BIGSERIAL     PRIMARY KEY,
    group_id         VARCHAR(32)   NOT NULL,
    user_id          UUID          NOT NULL,
    user_email       VARCHAR(255)  NOT NULL,
    trigger_message  TEXT          NOT NULL,
    stage_reached    VARCHAR(32)   NOT NULL,
    expense_created  BOOLEAN       NOT NULL DEFAULT FALSE,
    expense_id       BIGINT,
    total_amount     NUMERIC(18,2),
    currency         VARCHAR(3),
    description      TEXT,
    paid_by_email    VARCHAR(255),
    split_type       VARCHAR(16),
    claude_turns     INT           NOT NULL DEFAULT 0,
    error_message    TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ai_log_group_user ON aidb.ai_interaction_log (group_id, user_id);
CREATE INDEX IF NOT EXISTS idx_ai_log_created_at ON aidb.ai_interaction_log (created_at DESC);
