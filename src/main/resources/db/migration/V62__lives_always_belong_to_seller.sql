-- Module: live / analytics
-- Every live is started from the seller's account: there are no "store lives" anymore.
-- The store is an optional sales channel (catalog) of the seller (stores.user_id is UNIQUE),
-- so a live's owner is lives.seller_id alone. Drops the store/context columns added in
-- V4/V25 and the denormalized live_summaries.store_id from V34.

ALTER TABLE lives DROP CONSTRAINT IF EXISTS live_context_consistency;

-- idx_lives_store_id (V4) and the lives.store_id -> stores(id) FK go away with the column.
DROP INDEX IF EXISTS idx_lives_store_id;

ALTER TABLE lives
    DROP COLUMN IF EXISTS live_context,
    DROP COLUMN IF EXISTS store_id;

ALTER TABLE live_summaries
    DROP COLUMN IF EXISTS store_id;
