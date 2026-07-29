CREATE TABLE buyer_stripe_customers (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL UNIQUE REFERENCES users(id),
    stripe_customer_id VARCHAR(255) NOT NULL UNIQUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
