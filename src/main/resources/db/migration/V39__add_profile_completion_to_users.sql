ALTER TABLE users
    ADD COLUMN address_requirement_met BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payment_requirement_met BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN profile_complete         BOOLEAN NOT NULL DEFAULT FALSE;
