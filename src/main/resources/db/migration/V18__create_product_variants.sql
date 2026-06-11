CREATE TABLE product_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_option_name UNIQUE (product_id, name)
);

CREATE TABLE product_option_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id UUID NOT NULL REFERENCES product_options(id) ON DELETE CASCADE,
    value VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_option_value UNIQUE (option_id, value)
);

CREATE TABLE product_variants (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id     UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku            VARCHAR(100),
    price_override NUMERIC(10,2),
    is_default     BOOLEAN      NOT NULL DEFAULT false,
    position       INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_one_default_variant_per_product
    ON product_variants(product_id) WHERE is_default = true;

CREATE TABLE product_variant_options (
    variant_id      UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    option_value_id UUID NOT NULL REFERENCES product_option_values(id) ON DELETE CASCADE,
    PRIMARY KEY (variant_id, option_value_id)
);

CREATE INDEX idx_product_options_product  ON product_options(product_id);
CREATE INDEX idx_option_values_option     ON product_option_values(option_id);
CREATE INDEX idx_product_variants_product ON product_variants(product_id);
