package com.livecomerce.auth.infrastructure.cache;

import java.util.Optional;
import java.util.UUID;

final class StreamTickets {

    private StreamTickets() {}

    /** A missing or corrupt stored value redeems nothing. */
    static Optional<UUID> parseUserId(String stored) {
        if (stored == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(stored));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
