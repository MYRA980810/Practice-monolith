package com.livecomerce.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.StreamTicketPort;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fail-closed behaviour of {@link StreamTicketAuthFilter} when the ticket store or the user store
 * is down: the request is answered with 503 right here, the chain never runs and nobody is
 * authenticated.
 */
class StreamTicketAuthFilterTest {

    private final StreamTicketPort streamTicketPort = mock(StreamTicketPort.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);

    private StreamTicketAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new StreamTicketAuthFilter(streamTicketPort, userDetailsService,
                new RestAuthenticationEntryPoint(new ObjectMapper()));
        request = new MockHttpServletRequest("GET", StreamTicketAuthFilter.STREAM_PATH);
        request.addHeader(StreamTicketAuthFilter.TICKET_HEADER, "t");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void consumeFailure_answers503_withoutAuthenticatingOrContinuingTheChain() throws Exception {
        when(streamTicketPort.consume("t"))
                .thenThrow(new StreamTicketStoreUnavailableException("down", new QueryTimeoutException("slow")));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getContentAsString()).contains("\"code\":\"TICKET_STORE_UNAVAILABLE\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void userLookupInfraFailure_answers503_withoutAuthenticatingOrContinuingTheChain() throws Exception {
        var userId = UUID.randomUUID();
        when(streamTicketPort.consume("t")).thenReturn(Optional.of(userId));
        when(userDetailsService.loadUserByUsername(userId.toString()))
                .thenThrow(new CannotCreateTransactionException("no connection"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getContentAsString()).contains("\"code\":\"TICKET_STORE_UNAVAILABLE\"");
        assertThat(response.getContentAsString()).doesNotContain("TICKET_INVALID");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }
}
