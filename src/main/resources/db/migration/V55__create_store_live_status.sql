-- Local event-driven projection of "does this store have an active live right now".
-- Upserted by StoreLiveStatusEventListener from live's LiveStartedEvent/LiveEndedEvent,
-- so a store-card/listing read never crosses into the `live` module at request time.
-- One row per store: assumes a store runs at most one live at a time. No FK to `lives`
-- (different bounded context; live_id is populated straight from the domain event).
--
-- The row is never deleted once created: `is_live` is the current status, and
-- `last_event_at` (the event's own started_at/ended_at, not processing time) is
-- compared against every incoming event so an out-of-order redelivery (a delayed
-- LiveStartedEvent retry landing after its LiveEndedEvent already applied) is
-- detected and ignored instead of resurrecting a stale "live" state.
CREATE TABLE store_live_status (
    store_id      UUID PRIMARY KEY REFERENCES stores(id),
    live_id       UUID,
    is_live       BOOLEAN NOT NULL DEFAULT FALSE,
    last_event_at TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_store_live_status_live_id ON store_live_status(live_id);
