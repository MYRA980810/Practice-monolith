-- Existing options default to OTHER since historical data has no type.
ALTER TABLE product_options ADD COLUMN option_type VARCHAR(20) NOT NULL DEFAULT 'OTHER';
