CREATE TABLE seller_addresses (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    street       VARCHAR(255) NOT NULL,
    ext_number   VARCHAR(20),
    int_number   VARCHAR(20),
    neighborhood VARCHAR(100),
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    zip_code     VARCHAR(10)  NOT NULL,
    country      VARCHAR(3)   NOT NULL DEFAULT 'MX',
    is_default   BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_addresses_user_id ON seller_addresses(user_id);
