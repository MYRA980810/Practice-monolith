CREATE TABLE live_subscriptions (
    live_id UUID NOT NULL REFERENCES lives(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscribed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (live_id, buyer_id)
);
CREATE INDEX idx_live_subscriptions_live_id ON live_subscriptions(live_id);
CREATE INDEX idx_live_subscriptions_buyer_id ON live_subscriptions(buyer_id);
