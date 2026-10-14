package com.livecomerce.live.application;

import com.livecomerce.live.StreamTicketPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IssueFeedStreamTicketServiceTest {

    private final StreamTicketPort streamTicketPort = mock(StreamTicketPort.class);
    private final IssueFeedStreamTicketService service = new IssueFeedStreamTicketService(streamTicketPort);

    @Test
    void issue_returnsTheTicketBoundToTheUserWithItsLifetimeInMs() {
        var userId = UUID.randomUUID();
        when(streamTicketPort.issue(userId))
                .thenReturn(new StreamTicketPort.StreamTicket("opaque", Duration.ofSeconds(30)));

        var result = service.issue(userId);

        assertThat(result.ticket()).isEqualTo("opaque");
        assertThat(result.expiresInMs()).isEqualTo(30_000L);
    }
}
