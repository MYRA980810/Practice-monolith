-- Module: catalog
CREATE TABLE products (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id    UUID          NOT NULL REFERENCES stores(id),
    name        VARCHAR(255)  NOT NULL,
    description TEXT,
    base_price  NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    sku         VARCHAR(100),
    is_active   BOOLEAN       NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE product_images (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url        VARCHAR(500) NOT NULL,
    position   INTEGER      NOT NULL DEFAULT 0,
    is_primary BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE stocks (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id         UUID        NOT NULL UNIQUE REFERENCES products(id),
    total_quantity     INTEGER     NOT NULL DEFAULT 0,
    available_quantity INTEGER     NOT NULL DEFAULT 0,
    reserved_quantity  INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT stock_non_negative CHECK (
        available_quantity >= 0 AND reserved_quantity >= 0 AND total_quantity >= 0
    ),
    CONSTRAINT stock_totals_valid CHECK (
        available_quantity + reserved_quantity <= total_quantity
    )
);

CREATE INDEX idx_products_store_id ON products(store_id);
CREATE INDEX idx_stocks_product_id ON stocks(product_id);
