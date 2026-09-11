-- Module: analytics
CREATE TABLE store_ranking_snapshots (
    id                  UUID          PRIMARY KEY,
    store_id            UUID          NOT NULL REFERENCES stores(id),
    rank                INTEGER       NOT NULL,
    score               NUMERIC(10,6) NOT NULL,
    avg_rating          NUMERIC(3,2)  NOT NULL,
    review_count        INTEGER       NOT NULL,
    follower_count      BIGINT        NOT NULL,
    recent_sales_volume NUMERIC(14,2) NOT NULL,
    computed_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_ranking_computed_at ON store_ranking_snapshots(computed_at);
