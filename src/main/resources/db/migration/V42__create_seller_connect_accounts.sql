CREATE TABLE seller_connect_accounts (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL UNIQUE REFERENCES users(id),
    stripe_account_id  VARCHAR(255) NOT NULL UNIQUE,
    charges_enabled    BOOLEAN      NOT NULL DEFAULT false,
    payouts_enabled    BOOLEAN      NOT NULL DEFAULT false,
    details_submitted  BOOLEAN      NOT NULL DEFAULT false,
    activated_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
