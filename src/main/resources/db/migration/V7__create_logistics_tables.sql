-- Module: logistics
CREATE TABLE shipments (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID          NOT NULL UNIQUE REFERENCES orders(id),
    carrier        VARCHAR(15)   NOT NULL
                                 CHECK (carrier IN ('SKYDROPX', 'ESTAFETA', 'FEDEX', '99MINUTOS', 'SENDEX', 'DHL')),
    guide_number   VARCHAR(100)  UNIQUE,
    label_url      VARCHAR(500),
    carrier_cost   NUMERIC(10,2),
    charged_amount NUMERIC(10,2),
    margin_amount  NUMERIC(10,2),
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN (
                                     'PENDING', 'LABEL_GENERATED', 'PICKED_UP',
                                     'IN_TRANSIT', 'DELIVERED', 'FAILED'
                                 )),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE tracking_events (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID         NOT NULL REFERENCES shipments(id),
    status      VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    location    VARCHAR(255),
    occurred_at TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipments_guide    ON shipments(guide_number);
CREATE INDEX idx_tracking_shipment  ON tracking_events(shipment_id);
