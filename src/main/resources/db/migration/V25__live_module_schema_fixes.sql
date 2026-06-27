ALTER TABLE lives
    ADD COLUMN seller_id UUID NOT NULL REFERENCES users(id),
    ADD COLUMN live_context VARCHAR(20) NOT NULL DEFAULT 'STORE'
        CHECK (live_context IN ('STORE', 'SELLER_PROFILE')),
    ADD CONSTRAINT live_context_consistency CHECK (
        (live_context = 'STORE' AND store_id IS NOT NULL) OR
        (live_context = 'SELLER_PROFILE' AND store_id IS NULL)
    ),
    ALTER COLUMN store_id DROP NOT NULL;

ALTER TABLE live_products
    ALTER COLUMN product_id DROP NOT NULL,
    ADD COLUMN variant_id UUID REFERENCES product_variants(id),
    ADD COLUMN is_hot BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN display_duration_seconds INT NOT NULL DEFAULT 30,
    DROP CONSTRAINT stock_sold_valid,
    ADD CONSTRAINT stock_sold_valid CHECK (
        is_hot = true OR stock_sold <= stock_allocated
    );

ALTER TABLE live_products DROP CONSTRAINT IF EXISTS live_products_live_id_product_id_key;
DROP INDEX IF EXISTS live_products_live_id_product_id_key;
CREATE UNIQUE INDEX uq_live_products_catalog ON live_products(live_id, product_id)
    WHERE product_id IS NOT NULL;

ALTER TABLE live_products
    ADD CONSTRAINT live_product_type_consistency CHECK (
        (is_hot = false AND product_id IS NOT NULL AND variant_id IS NOT NULL) OR
        (is_hot = true AND product_id IS NULL AND variant_id IS NULL)
    );
