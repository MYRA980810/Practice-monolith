ALTER TABLE lives
    ADD COLUMN ivs_channel_arn      VARCHAR(255) UNIQUE,
    ADD COLUMN ivs_ingest_endpoint  VARCHAR(255),
    ADD COLUMN ivs_stream_key_arn   VARCHAR(255),
    ADD COLUMN ivs_stream_key_value TEXT,
    ADD COLUMN ivs_playback_url     VARCHAR(500);

CREATE INDEX idx_lives_ivs_channel_arn ON lives(ivs_channel_arn);
