ALTER TABLE order_items
    ALTER COLUMN product_id DROP NOT NULL,
    ALTER COLUMN variant_id DROP NOT NULL,
    ADD COLUMN item_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT'
        CHECK (item_type IN ('PRODUCT', 'HOT_PRODUCT', 'SHIPPING')),
    ADD CONSTRAINT order_item_type_consistency CHECK (
        (item_type = 'PRODUCT' AND product_id IS NOT NULL AND variant_id IS NOT NULL) OR
        (item_type IN ('HOT_PRODUCT', 'SHIPPING') AND product_id IS NULL AND variant_id IS NULL)
    );

ALTER TABLE orders
    ALTER COLUMN store_id DROP NOT NULL,
    ADD COLUMN shipping_payment_status VARCHAR(20) DEFAULT NULL
        CHECK (shipping_payment_status IN ('PENDING', 'PAID', 'FAILED'));
