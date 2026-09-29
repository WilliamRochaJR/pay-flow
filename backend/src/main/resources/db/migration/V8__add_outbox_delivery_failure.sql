ALTER TABLE outbox_events
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_error VARCHAR(500),
    ADD COLUMN exhausted_at TIMESTAMPTZ;

DROP INDEX outbox_events_pending_idx;

CREATE INDEX outbox_events_pending_idx
    ON outbox_events (created_at, event_id)
    WHERE published_at IS NULL AND exhausted_at IS NULL;
