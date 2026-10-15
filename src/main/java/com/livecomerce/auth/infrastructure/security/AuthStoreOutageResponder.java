package com.livecomerce.auth.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Answers a request with 503 when an auth filter can't reach a store it authenticates against
 * (user store, ticket store). Shared by {@link JwtAuthFilter} and {@link StreamTicketAuthFilter} so
 * every outage is answered, counted and logged the same way.
 * <p>
 * During an outage every request fails, so the full stack trace is logged at most once per
 * {@link #STACK_TRACE_WINDOW} per filter; the rest get a one-line warn. Each 503 increments the
 * {@value #METRIC} counter, tagged with the filter, so the outage stays visible in metrics.
 */
@RequiredArgsConstructor
class AuthStoreOutageResponder {

    static final String METRIC = "auth.store.unavailable";
    static final Duration STACK_TRACE_WINDOW = Duration.ofSeconds(30);

    private static final long NEVER = Long.MIN_VALUE;

    private final RestAuthenticationEntryPoint errorWriter;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final ConcurrentMap<String, AtomicLong> lastStackTraceAt = new ConcurrentHashMap<>();

    /**
     * Fails closed: clears the security context and writes the 503 body with {@code Retry-After}.
     * The caller must not continue the filter chain.
     *
     * @param log     the calling filter's logger, so the log category stays the filter's
     * @param filter  metric tag and throttle key, e.g. {@code jwt}
     * @param store   which store failed, for the log line
     * @return whether this call logged the full stack trace
     */
    boolean reject(HttpServletResponse response, Logger log, String filter, String store, String errorCode,
                   Duration retryAfter, Exception cause) throws IOException {
        Counter.builder(METRIC)
                .description("Requests answered 503 because an auth store was unavailable")
                .tag("filter", filter)
                .register(meterRegistry)
                .increment();

        var withStackTrace = claimStackTrace(filter);
        if (withStackTrace) {
            log.error("Authentication unavailable ({} failure); answering 503 {}", store, errorCode, cause);
        } else {
            log.warn("Authentication unavailable ({} failure: {}: {}); answering 503 {} (stack trace throttled)",
                    store, cause.getClass().getSimpleName(), cause.getMessage(), errorCode);
        }

        SecurityContextHolder.clearContext();
        errorWriter.writeUnavailable(response, errorCode, retryAfter);
        return withStackTrace;
    }

    /** True for the first failure of a window; concurrent callers race and only one wins. */
    private boolean claimStackTrace(String key) {
        var last = lastStackTraceAt.computeIfAbsent(key, k -> new AtomicLong(NEVER));
        var now = clock.millis();
        var previous = last.get();
        if (previous != NEVER && now - previous < STACK_TRACE_WINDOW.toMillis()) {
            return false;
        }
        return last.compareAndSet(previous, now);
    }
}
