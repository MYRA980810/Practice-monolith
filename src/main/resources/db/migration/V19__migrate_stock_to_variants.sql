INSERT INTO product_variants (id, product_id, sku, is_default, created_at, updated_at)
SELECT gen_random_uuid(), p.id, p.sku, true, NOW(), NOW() FROM products p;

ALTER TABLE stocks ADD COLUMN variant_id UUID REFERENCES product_variants(id);

UPDATE stocks s SET variant_id = v.id
FROM product_variants v WHERE v.product_id = s.product_id AND v.is_default = true;

INSERT INTO stocks (id, variant_id, total_quantity, available_quantity, reserved_quantity, created_at, updated_at)
SELECT gen_random_uuid(), v.id, 0, 0, 0, NOW(), NOW()
FROM product_variants v
WHERE v.is_default = true
  AND NOT EXISTS (SELECT 1 FROM stocks s2 WHERE s2.variant_id = v.id);

ALTER TABLE stocks ALTER COLUMN variant_id SET NOT NULL;
ALTER TABLE stocks ADD CONSTRAINT uq_stocks_variant UNIQUE (variant_id);
ALTER TABLE stocks DROP COLUMN product_id;
CREATE INDEX idx_stocks_variant_id ON stocks(variant_id);

ALTER TABLE order_items ADD COLUMN variant_id UUID REFERENCES product_variants(id);

UPDATE order_items oi SET variant_id = v.id
FROM product_variants v WHERE v.product_id = oi.product_id AND v.is_default = true;

ALTER TABLE order_items ALTER COLUMN variant_id SET NOT NULL;
CREATE INDEX idx_order_items_variant ON order_items(variant_id);

ALTER TABLE products DROP COLUMN sku;
