-- Module: store (parte de auth/catalog)
CREATE TABLE stores (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL UNIQUE REFERENCES users(id),
    name            VARCHAR(255)  NOT NULL,
    slug            VARCHAR(255)  NOT NULL UNIQUE,
    description     TEXT,
    logo_url        VARCHAR(500),
    plan            VARCHAR(10)   NOT NULL DEFAULT 'FREE'
                                  CHECK (plan IN ('FREE', 'STARTER', 'PRO', 'AGENCY')),
    commission_rate NUMERIC(5,4)  NOT NULL DEFAULT 0.05,
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE subscriptions (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id      UUID          NOT NULL REFERENCES stores(id),
    plan          VARCHAR(10)   NOT NULL,
    price_amount  NUMERIC(10,2) NOT NULL,
    currency      VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    status        VARCHAR(10)   NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED')),
    starts_at     TIMESTAMPTZ   NOT NULL,
    ends_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer_addresses (
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

CREATE INDEX idx_stores_slug    ON stores(slug);
CREATE INDEX idx_stores_user_id ON stores(user_id);
