package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStreamTicketAdapterTest {

    private static final long TTL_MS = 30_000;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-13T10:00:00Z"));
    private final InMemoryStreamTicketAdapter adapter =
            new InMemoryStreamTicketAdapter(new RefreshTokenHasher(), clock, TTL_MS);

    @Test
    void ticketIsRedeemableExactlyOnce() {
        var userId = UUID.randomUUID();
        var ticket = adapter.issue(userId);

        assertThat(ticket.ttl()).isEqualTo(Duration.ofMillis(TTL_MS));
        assertThat(adapter.consume(ticket.value())).contains(userId);
        assertThat(adapter.consume(ticket.value())).isEmpty();
    }

    @Test
    void expiredTicketIsRejected() {
        var ticket = adapter.issue(UUID.randomUUID());

        clock.advance(Duration.ofMillis(TTL_MS + 1));

        assertThat(adapter.consume(ticket.value())).isEmpty();
    }

    @Test
    void unknownOrBlankTicketIsRejected() {
        assertThat(adapter.consume("unknown")).isEmpty();
        assertThat(adapter.consume(null)).isEmpty();
        assertThat(adapter.consume("")).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
