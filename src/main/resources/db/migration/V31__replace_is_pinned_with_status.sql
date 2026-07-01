ALTER TABLE live_products ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';
UPDATE live_products SET status = 'PINNED' WHERE is_pinned = true;
ALTER TABLE live_products DROP COLUMN is_pinned;
