ALTER TABLE transfers ADD COLUMN owner_id UUID REFERENCES users(id);
ALTER TABLE transfers ADD COLUMN idempotency_key UUID;

UPDATE transfers t
SET owner_id = a.owner_id
FROM accounts a
WHERE t.source_account_id = a.id
  AND t.owner_id IS NULL;

ALTER TABLE transfers ALTER COLUMN owner_id SET NOT NULL;
CREATE UNIQUE INDEX transfers_owner_idempotency_key_uidx
    ON transfers (owner_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
