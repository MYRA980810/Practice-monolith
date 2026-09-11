-- Module: review
CREATE TABLE reviews (
    order_id                     UUID          PRIMARY KEY REFERENCES orders(id),
    store_id                     UUID          NOT NULL REFERENCES stores(id),
    buyer_id                     UUID          NOT NULL REFERENCES users(id),
    description_accuracy_rating  INTEGER       NOT NULL CHECK (description_accuracy_rating BETWEEN 1 AND 5),
    packaging_condition_rating   INTEGER       NOT NULL CHECK (packaging_condition_rating BETWEEN 1 AND 5),
    delivery_timeliness_rating   INTEGER       NOT NULL CHECK (delivery_timeliness_rating BETWEEN 1 AND 5),
    seller_attention_rating      INTEGER       NOT NULL CHECK (seller_attention_rating BETWEEN 1 AND 5),
    ranking_impact_score         NUMERIC(3,2)  NOT NULL,
    comment                      VARCHAR(300),
    anonymous                    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at                   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE review_photos (
    id              UUID         PRIMARY KEY,
    review_order_id UUID         NOT NULL REFERENCES reviews(order_id),
    url             VARCHAR(500) NOT NULL,
    position        INTEGER      NOT NULL
);

CREATE TABLE product_ratings (
    id               UUID    PRIMARY KEY,
    review_order_id  UUID    NOT NULL REFERENCES reviews(order_id),
    order_item_id    UUID    NOT NULL REFERENCES order_items(id),
    product_id       UUID    NOT NULL REFERENCES products(id),
    rating           INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uq_product_ratings_order_item UNIQUE (order_item_id)
);

CREATE INDEX idx_reviews_store_id        ON reviews(store_id);
CREATE INDEX idx_reviews_buyer_id        ON reviews(buyer_id);
CREATE INDEX idx_review_photos_review    ON review_photos(review_order_id);
CREATE INDEX idx_product_ratings_review  ON product_ratings(review_order_id);
CREATE INDEX idx_product_ratings_product ON product_ratings(product_id);
