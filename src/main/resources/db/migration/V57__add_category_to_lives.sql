-- Optional category per live session so buyers can browse lives by category.
-- ON DELETE SET NULL: removing a category must never cascade into live history.
ALTER TABLE lives ADD COLUMN category_id UUID NULL REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX idx_lives_status_category ON lives(status, category_id);
