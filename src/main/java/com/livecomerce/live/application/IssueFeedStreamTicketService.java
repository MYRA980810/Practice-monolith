package com.livecomerce.live.application;

import com.livecomerce.live.StreamTicketPort;
import com.livecomerce.live.application.port.in.IssueFeedStreamTicketUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The browser can't send the Bearer token (it lives in an httpOnly cookie of the Next.js app), so
 * the Next.js server trades it for a short-lived, single-use ticket that the browser presents in
 * {@code X-Stream-Ticket} when it opens {@code GET /api/lives/feed/stream}. A new ticket per connection.
 */
@Service
@RequiredArgsConstructor
public class IssueFeedStreamTicketService implements IssueFeedStreamTicketUseCase {

    private final StreamTicketPort streamTicketPort;

    @Override
    public FeedStreamTicket issue(UUID userId) {
        var ticket = streamTicketPort.issue(userId);
        return new FeedStreamTicket(ticket.value(), ticket.ttl().toMillis());
    }
}
