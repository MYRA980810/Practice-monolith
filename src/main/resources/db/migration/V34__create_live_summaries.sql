-- Module: analytics
CREATE TABLE live_summaries (
    live_id          UUID          PRIMARY KEY REFERENCES lives(id),
    seller_id        UUID          NOT NULL,
    store_id         UUID,
    started_at       TIMESTAMPTZ,
    ended_at         TIMESTAMPTZ,
    duration_seconds BIGINT        NOT NULL,
    peak_viewers     INTEGER       NOT NULL,
    total_sales      NUMERIC(10,2) NOT NULL DEFAULT 0,
    order_count      INTEGER       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE live_summary_orders (
    id          UUID          PRIMARY KEY,
    live_id     UUID          NOT NULL REFERENCES live_summaries(live_id) ON DELETE CASCADE,
    order_id    UUID          NOT NULL,
    buyer_id    UUID          NOT NULL,
    item_names  TEXT,
    order_total NUMERIC(10,2) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (live_id, order_id)
);

CREATE INDEX idx_live_summary_orders_live ON live_summary_orders(live_id);
