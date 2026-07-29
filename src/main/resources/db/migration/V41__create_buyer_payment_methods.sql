CREATE TABLE buyer_payment_methods (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL REFERENCES users(id),
    stripe_payment_method_id VARCHAR(255) NOT NULL UNIQUE,
    brand                    VARCHAR(30)  NOT NULL,
    last4                    VARCHAR(4)   NOT NULL,
    exp_month                SMALLINT     NOT NULL,
    exp_year                 SMALLINT     NOT NULL,
    is_default               BOOLEAN      NOT NULL DEFAULT false,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_buyer_payment_methods_user_id ON buyer_payment_methods(user_id);
