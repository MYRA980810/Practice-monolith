package com.livecomerce.live;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Single-use, short-lived ticket that lets a browser open the SSE lives feed
 * ({@code GET /api/lives/feed/stream}) without a Bearer header: the Next.js server, which holds
 * the JWT, issues a ticket and the browser presents it in {@code X-Stream-Ticket}.
 * Defined by {@code live}, implemented by {@code auth} (which also authenticates with it).
 */
public interface StreamTicketPort {

    /** Issues a new opaque ticket bound to {@code userId}. */
    StreamTicket issue(UUID userId);

    /**
     * Atomically redeems a ticket: returns its user the first time, empty if the ticket is unknown,
     * expired or already used.
     */
    Optional<UUID> consume(String ticket);

    record StreamTicket(String value, Duration ttl) {}
}
