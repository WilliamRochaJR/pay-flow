INSERT INTO users (id, name, email, password_hash, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Contas de demonstração legadas',
    'legacy-demo@payflow.invalid',
    '{disabled}',
    NOW()
);

ALTER TABLE accounts ADD COLUMN owner_id UUID;

UPDATE accounts
SET owner_id = '00000000-0000-0000-0000-000000000001'
WHERE owner_id IS NULL;

ALTER TABLE accounts ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE accounts ADD CONSTRAINT accounts_owner_fk FOREIGN KEY (owner_id) REFERENCES users(id);
CREATE INDEX accounts_owner_id_idx ON accounts (owner_id);
