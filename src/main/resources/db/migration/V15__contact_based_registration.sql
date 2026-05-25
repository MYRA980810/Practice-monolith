-- Support phone-only registration: make email nullable, enforce uniqueness via partial indexes.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
DROP INDEX IF EXISTS idx_users_email;
CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE email IS NOT NULL;

-- Nullify duplicate phones before enforcing uniqueness (cleans up dev test data).
UPDATE users SET phone = NULL
WHERE phone IS NOT NULL AND phone IN (
    SELECT phone FROM users GROUP BY phone HAVING COUNT(*) > 1
);
CREATE UNIQUE INDEX idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
