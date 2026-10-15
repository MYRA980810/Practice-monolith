package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.live.StreamTicketPort;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates {@code GET /api/lives/feed/stream} with a single-use {@code X-Stream-Ticket} (see
 * {@link StreamTicketPort}), for browsers that can't send the Bearer token. The header is ignored on
 * any other request, so a ticket is never a general credential, and it is never read as a JWT (nor a
 * JWT as a ticket). The user is reloaded like {@link JwtAuthFilter} does, so the principal and
 * authorities are identical and the stream's {@code hasRole('BUYER')} applies.
 * <p>
 * A bad ticket (unknown, expired or already used) sets {@code auth_error=TICKET_INVALID} and the
 * chain continues: {@link RestAuthenticationEntryPoint} answers 401 with that code, telling the
 * client to fetch a new ticket instead of refreshing its session.
 * <p>
 * If the ticket store or the user store is down, the request is answered right here with 503
 * {@code TICKET_STORE_UNAVAILABLE} and {@code Retry-After} (never {@code TICKET_INVALID}, which
 * would send the client into a ticket-fetching loop against a dead store), and nobody is
 * authenticated.
 * <p>
 * Runs once per request: {@link OncePerRequestFilter} skips the ASYNC/ERROR re-dispatches of the SSE
 * response, so the ticket is redeemed only on the initial dispatch.
 */
@RequiredArgsConstructor
class StreamTicketAuthFilter extends OncePerRequestFilter {

    static final String STREAM_PATH = "/api/lives/feed/stream";
    static final String TICKET_HEADER = "X-Stream-Ticket";
    static final String TICKET_INVALID = "TICKET_INVALID";

    private static final Logger log = LoggerFactory.getLogger(StreamTicketAuthFilter.class);

    private final StreamTicketPort streamTicketPort;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint errorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod())
                || request.getHeader(TICKET_HEADER) == null
                || !STREAM_PATH.equals(pathWithinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                authenticate(request);
            } catch (StreamTicketStoreUnavailableException e) {
                rejectAsUnavailable(response, "ticket store", e);
                return;
            } catch (DataAccessException | TransactionException e) {
                // The ticket was already redeemed (GETDEL) when the user lookup failed, so it is
                // burned: the client needs a new ticket anyway, which is fine because 503 makes it
                // back off and request one. Never TICKET_INVALID: that would hide the outage.
                rejectAsUnavailable(response, "user store", e);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        var userId = streamTicketPort.consume(request.getHeader(TICKET_HEADER));
        if (userId.isEmpty()) {
            log.warn("Rejected stream ticket: unknown, expired or already used");
            request.setAttribute("auth_error", TICKET_INVALID);
            return;
        }
        try {
            var userDetails = userDetailsService.loadUserByUsername(userId.get().toString());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
            log.debug("Authenticated user '{}' with a stream ticket", userId.get());
        } catch (UsernameNotFoundException e) {
            log.warn("Stream ticket for unknown user '{}'", userId.get());
            request.setAttribute("auth_error", TICKET_INVALID);
        }
    }

    /**
     * Fails closed: nothing was authenticated and the chain is not continued, so the request can't
     * reach the stream. Answered here because this filter runs before the
     * {@code ExceptionTranslationFilter} and no {@code @RestControllerAdvice} applies.
     */
    private void rejectAsUnavailable(HttpServletResponse response, String store, Exception e) throws IOException {
        log.error("Stream ticket authentication unavailable ({} failure: {}); answering 503 {}",
                store, e.getClass().getSimpleName(), StreamTicketStoreUnavailableException.CODE);
        SecurityContextHolder.clearContext();
        errorWriter.writeUnavailable(response, StreamTicketStoreUnavailableException.CODE,
                StreamTicketStoreUnavailableException.RETRY_AFTER);
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        var uri = request.getRequestURI();
        var contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }
}
