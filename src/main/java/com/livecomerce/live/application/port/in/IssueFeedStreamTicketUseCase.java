package com.livecomerce.live.application.port.in;

import java.util.UUID;

/** Issues the single-use ticket that opens the SSE lives feed without a Bearer header. */
public interface IssueFeedStreamTicketUseCase {

    FeedStreamTicket issue(UUID userId);

    record FeedStreamTicket(String ticket, long expiresInMs) {}
}
