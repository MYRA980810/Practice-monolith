-- Analytics module: indexes for aggregation queries
CREATE INDEX idx_order_items_product_status ON order_items(product_id, status);
CREATE INDEX idx_order_items_paid_at ON order_items(paid_at) WHERE status = 'PAID';
