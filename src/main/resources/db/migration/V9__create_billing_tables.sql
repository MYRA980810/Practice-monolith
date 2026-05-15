CREATE TABLE subscriptions (
    id                   UUID        NOT NULL,
    user_id              UUID        NOT NULL,
    plan                 VARCHAR(10) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    gateway              VARCHAR(20) NOT NULL,
    external_id          VARCHAR(255),
    current_period_start TIMESTAMPTZ NOT NULL,
    current_period_end   TIMESTAMPTZ NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT uq_subscriptions_user_id UNIQUE (user_id)
);
