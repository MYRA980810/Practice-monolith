INSERT INTO seller_addresses (user_id, street, ext_number, int_number, neighborhood,
    city, state, zip_code, country, is_default)
SELECT user_id, shipping_street, shipping_ext_number, shipping_int_number, shipping_neighborhood,
    shipping_city, shipping_state, shipping_zip_code, COALESCE(shipping_country, 'MX'), true
FROM stores
WHERE shipping_street IS NOT NULL;

COMMENT ON COLUMN stores.shipping_street IS
    'DEPRECATED: superseded by seller_addresses (keyed by user_id). Kept for rollback only; a future migration will DROP these shipping_* columns.';
