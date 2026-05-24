ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL,
    ALTER COLUMN role DROP NOT NULL,
    ADD COLUMN provider    VARCHAR(20),
    ADD COLUMN provider_id VARCHAR(255),
    ADD COLUMN avatar_url  VARCHAR(512);

CREATE UNIQUE INDEX idx_users_provider ON users(provider, provider_id)
    WHERE provider IS NOT NULL;
