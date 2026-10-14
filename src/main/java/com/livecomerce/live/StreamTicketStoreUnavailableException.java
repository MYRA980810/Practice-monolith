package com.livecomerce.live;

import java.time.Duration;

/**
 * The {@link StreamTicketPort} store can't be reached (e.g. Redis is down or timing out), so a
 * ticket can be neither issued nor redeemed right now. This is an infrastructure outage, not a bad
 * ticket: callers must answer "retry later" (503), never "fetch a new ticket" (401
 * {@code TICKET_INVALID}). Adapters translate their store-specific failures into this exception so
 * neither {@code live} nor the stream filter depend on the store's types.
 */
public class StreamTicketStoreUnavailableException extends RuntimeException {

    /** Error code exposed to clients for this outage, on both the issue and the redeem path. */
    public static final String CODE = "TICKET_STORE_UNAVAILABLE";

    /** How long clients are asked to wait before retrying (sent as {@code Retry-After}). */
    public static final Duration RETRY_AFTER = Duration.ofSeconds(5);

    public StreamTicketStoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
