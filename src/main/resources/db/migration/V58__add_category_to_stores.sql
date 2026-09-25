-- Optional seller-chosen category override per store. When NULL, the store's effective
-- category is inferred from its active products at read time.
-- ON DELETE SET NULL: removing a category must fall back to inference, never delete stores.
ALTER TABLE stores ADD COLUMN category_id UUID NULL REFERENCES categories(id) ON DELETE SET NULL;
