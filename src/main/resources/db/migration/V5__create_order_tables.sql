-- Module: order
CREATE TABLE orders (
    id                        UUID          PRIMARY KEY,
    buyer_id                  UUID          NOT NULL REFERENCES users(id),
    live_id                   UUID          REFERENCES lives(id),
    store_id                  UUID          NOT NULL REFERENCES stores(id),
    status                    VARCHAR(15)   NOT NULL DEFAULT 'OPEN'
                                            CHECK (status IN (
                                                'OPEN', 'PAID',
                                                'SHIPPED', 'DELIVERED', 'CANCELLED'
                                            )),
    currency                  VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    shipping_address_snapshot JSONB,
    tracking_number           VARCHAR(100),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id             UUID          PRIMARY KEY,
    order_id       UUID          NOT NULL REFERENCES orders(id),
    product_id     UUID          NOT NULL REFERENCES products(id),
    product_name   VARCHAR(255)  NOT NULL,
    unit_price     NUMERIC(10,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    quantity       INTEGER       NOT NULL CHECK (quantity > 0),
    subtotal       NUMERIC(10,2) NOT NULL,
    status         VARCHAR(10)   NOT NULL DEFAULT 'RESERVED'
                                 CHECK (status IN ('RESERVED', 'PAID', 'EXPIRED')),
    reserved_until TIMESTAMPTZ,
    paid_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_buyer_id        ON orders(buyer_id);
CREATE INDEX idx_orders_live_id         ON orders(live_id);
CREATE INDEX idx_orders_store_id        ON orders(store_id);
CREATE INDEX idx_orders_status          ON orders(status);
CREATE INDEX idx_order_items_order      ON order_items(order_id);
CREATE INDEX idx_order_items_reserved   ON order_items(status, reserved_until)
    WHERE status = 'RESERVED';

-- Una sola orden por buyer por live session
CREATE UNIQUE INDEX idx_orders_unique_per_live
    ON orders(buyer_id, live_id)
    WHERE live_id IS NOT NULL;
