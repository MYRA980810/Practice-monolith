ALTER TABLE lives DROP CONSTRAINT IF EXISTS lives_ivs_channel_arn_key;

DROP INDEX IF EXISTS idx_lives_ivs_channel_arn;
CREATE INDEX idx_lives_ivs_channel_arn_status ON lives(ivs_channel_arn, status);
