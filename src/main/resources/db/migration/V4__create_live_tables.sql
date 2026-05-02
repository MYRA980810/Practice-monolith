-- Module: live
CREATE TABLE lives (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id         UUID         NOT NULL REFERENCES stores(id),
    title            VARCHAR(255) NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'SCHEDULED'
                                  CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED', 'CANCELLED')),
    agora_channel_id VARCHAR(255) UNIQUE,
    stream_token     TEXT,
    thumbnail_url    VARCHAR(500),
    scheduled_at     TIMESTAMPTZ,
    started_at       TIMESTAMPTZ,
    ended_at         TIMESTAMPTZ,
    peak_viewers     INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE live_products (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    live_id               UUID          NOT NULL REFERENCES lives(id),
    product_id            UUID          NOT NULL REFERENCES products(id),
    product_name_snapshot VARCHAR(255)  NOT NULL,
    price_snapshot        NUMERIC(10,2) NOT NULL,
    currency_snapshot     VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    stock_allocated       INTEGER       NOT NULL DEFAULT 0,
    stock_sold            INTEGER       NOT NULL DEFAULT 0,
    is_pinned             BOOLEAN       NOT NULL DEFAULT false,
    position              INTEGER       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE(live_id, product_id),
    CONSTRAINT stock_sold_valid CHECK (stock_sold <= stock_allocated)
);

CREATE INDEX idx_lives_store_id       ON lives(store_id);
CREATE INDEX idx_lives_status         ON lives(status);
CREATE INDEX idx_live_products_live   ON live_products(live_id);
CREATE INDEX idx_live_products_pinned ON live_products(live_id, is_pinned);
