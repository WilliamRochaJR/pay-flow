ALTER TABLE transfers
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL_TRANSFER',
    ADD COLUMN original_transfer_id UUID REFERENCES transfers(id);

ALTER TABLE transfers
    ADD CONSTRAINT transfers_type_check
        CHECK (type IN ('INTERNAL_TRANSFER', 'REVERSAL')),
    ADD CONSTRAINT transfers_reversal_link_check
        CHECK (
            (type = 'INTERNAL_TRANSFER' AND original_transfer_id IS NULL)
            OR (type = 'REVERSAL' AND original_transfer_id IS NOT NULL)
        );

CREATE UNIQUE INDEX transfers_original_transfer_id_uidx
    ON transfers (original_transfer_id)
    WHERE original_transfer_id IS NOT NULL;
