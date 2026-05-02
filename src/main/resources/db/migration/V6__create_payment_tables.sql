-- Module: payment
CREATE TABLE payments (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID          NOT NULL UNIQUE REFERENCES orders(id),
    provider       VARCHAR(10)   NOT NULL CHECK (provider IN ('CONEKTA', 'STRIPE')),
    method         VARCHAR(10)   NOT NULL CHECK (method IN ('OXXO', 'SPEI', 'CARD')),
    external_id    VARCHAR(255)  UNIQUE,
    status         VARCHAR(15)   NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN (
                                     'PENDING', 'PROCESSING', 'CONFIRMED', 'FAILED', 'REFUNDED'
                                 )),
    amount         NUMERIC(10,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'MXN',
    oxxo_reference VARCHAR(255),
    spei_clabe     VARCHAR(20),
    expires_at     TIMESTAMPTZ,
    confirmed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_external_id ON payments(external_id);
CREATE INDEX idx_payments_order_id    ON payments(order_id);
