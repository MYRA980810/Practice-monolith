-- Module: store
CREATE TABLE store_followers (
    store_id   UUID        NOT NULL REFERENCES stores(id),
    user_id    UUID        NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (store_id, user_id)
);

CREATE INDEX idx_store_followers_user ON store_followers(user_id);
