ALTER TABLE stores ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE stores ADD COLUMN suspension_reason VARCHAR(20);
ALTER TABLE stores ADD CONSTRAINT chk_suspension_reason
    CHECK (suspension_reason IN ('BILLING', 'POLICY_VIOLATION'));
