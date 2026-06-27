ALTER TABLE stores
    ADD COLUMN shipping_street       VARCHAR(255),
    ADD COLUMN shipping_ext_number   VARCHAR(20),
    ADD COLUMN shipping_int_number   VARCHAR(20),
    ADD COLUMN shipping_neighborhood VARCHAR(100),
    ADD COLUMN shipping_city         VARCHAR(100),
    ADD COLUMN shipping_state        VARCHAR(100),
    ADD COLUMN shipping_zip_code     VARCHAR(10),
    ADD COLUMN shipping_country      VARCHAR(3) DEFAULT 'MX';
