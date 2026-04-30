CREATE TABLE notification (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notification_user_created_at ON notification (user_id, created_at DESC);

CREATE TABLE notification_channel_state (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NULL,
    last_error TEXT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (notification_id, channel)
);

CREATE INDEX idx_channel_state_retry ON notification_channel_state (channel, state, next_retry_at);

CREATE TABLE user_notification_status (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notification_id UUID NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ NULL,
    is_clicked BOOLEAN NOT NULL DEFAULT FALSE,
    clicked_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, notification_id)
);

CREATE INDEX idx_user_status_user_read_notification ON user_notification_status (user_id, is_read, notification_id DESC);

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_outbox_unpublished_scan ON outbox (published_at, created_at);

CREATE TABLE notification_preference (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    inapp_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    event_type_rules_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    quiet_hours_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL
);
