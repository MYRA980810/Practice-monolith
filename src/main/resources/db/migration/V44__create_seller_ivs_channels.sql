CREATE TABLE seller_ivs_channels (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id          UUID         NOT NULL UNIQUE REFERENCES users(id),
    channel_arn        VARCHAR(255) NOT NULL UNIQUE,
    ingest_endpoint    VARCHAR(255),
    stream_key_arn     VARCHAR(255),
    stream_key_value   TEXT,
    playback_url       VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
