CREATE TABLE live_chat_messages (
  id UUID PRIMARY KEY,
  live_id UUID NOT NULL REFERENCES lives(id),
  agora_msg_id VARCHAR(255) NOT NULL,
  user_id UUID NOT NULL,
  username VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  sent_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_live_chat_agora_msg UNIQUE (agora_msg_id)
);

CREATE INDEX idx_live_chat_live_id_sent ON live_chat_messages(live_id, sent_at DESC);
