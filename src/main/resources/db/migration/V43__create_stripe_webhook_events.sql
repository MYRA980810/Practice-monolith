CREATE TABLE stripe_webhook_events (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id  VARCHAR(255) NOT NULL UNIQUE,
    type             VARCHAR(100) NOT NULL,
    processed_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
