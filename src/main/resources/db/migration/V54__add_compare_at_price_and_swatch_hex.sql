-- Additive columns, both nullable: no backfill needed, no impact on existing rows.

-- "Was" price shown struck-through next to base_price to advertise a discount.
-- Lives at the product level, not per-variant.
ALTER TABLE products ADD COLUMN compare_at_price NUMERIC(10, 2);

-- Only populated for option values that represent a color (e.g. an option
-- named "Color"). Sizes, materials, etc. never carry a swatch.
ALTER TABLE product_option_values ADD COLUMN swatch_hex VARCHAR(7);
