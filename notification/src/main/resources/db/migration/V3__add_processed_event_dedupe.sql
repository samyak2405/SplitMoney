CREATE TABLE processed_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    dedupe_key VARCHAR(200) NOT NULL UNIQUE
);

CREATE INDEX idx_processed_event_lookup ON processed_event (event_id, user_id, event_type);
