ALTER TABLE lives ADD COLUMN display_duration_seconds INT NOT NULL DEFAULT 60;
ALTER TABLE live_products DROP COLUMN display_duration_seconds;
