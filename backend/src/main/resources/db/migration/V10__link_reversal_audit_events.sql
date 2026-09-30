ALTER TABLE audit_events
    ADD COLUMN original_transfer_id UUID;

CREATE INDEX audit_events_original_transfer_id_idx
    ON audit_events (original_transfer_id)
    WHERE original_transfer_id IS NOT NULL;
