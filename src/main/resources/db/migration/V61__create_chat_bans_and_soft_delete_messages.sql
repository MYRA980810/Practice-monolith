-- Chat bans are per seller (not per live): a banned user stays banned in the seller's future lives.
-- Idempotency: the application inserts with INSERT ... ON CONFLICT (seller_id, user_id) DO NOTHING,
-- so re-banning keeps the original row and never raises a constraint violation.
-- user_id has no FK to users, consistent with live_chat_messages.user_id: the id comes from the chat
-- (the IVS token userId) and the ban is stored as-is, without a users lookup.
CREATE TABLE chat_bans (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id  UUID         NOT NULL REFERENCES users(id),
    user_id    UUID         NOT NULL,
    reason     VARCHAR(256),
    banned_by  UUID         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_chat_bans_seller_user UNIQUE (seller_id, user_id)
);

-- Soft delete of moderated messages; history queries skip rows where deleted_at is set.
ALTER TABLE live_chat_messages ADD COLUMN deleted_at TIMESTAMPTZ NULL;
