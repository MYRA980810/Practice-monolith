CREATE INDEX idx_lives_seller_id ON lives(seller_id);
CREATE INDEX idx_live_products_hot_type ON live_products(live_id, is_hot);
CREATE INDEX idx_orders_shipping_status ON orders(shipping_payment_status)
    WHERE shipping_payment_status IS NOT NULL;
