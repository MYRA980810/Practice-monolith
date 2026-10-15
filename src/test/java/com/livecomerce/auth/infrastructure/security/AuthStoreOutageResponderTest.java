package com.livecomerce.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthStoreOutageResponderTest {

    private static final Logger log = LoggerFactory.getLogger(AuthStoreOutageResponderTest.class);
    private static final Exception FAILURE = new DataAccessResourceFailureException("db down");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-14T12:00:00Z"));
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private AuthStoreOutageResponder responder;

    @BeforeEach
    void setUp() {
        responder = new AuthStoreOutageResponder(new RestAuthenticationEntryPoint(new ObjectMapper()),
                meterRegistry, clock);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reject_answers503WithRetryAfterAndCode_andClearsTheSecurityContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", null, java.util.List.of()));
        var response = new MockHttpServletResponse();

        responder.reject(response, log, "jwt", "user store", "SOME_CODE", Duration.ofSeconds(7), FAILURE);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("7");
        assertThat(response.getContentAsString()).contains("\"code\":\"SOME_CODE\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void reject_incrementsTheOutageCounterTaggedByFilter() throws Exception {
        reject("jwt");
        reject("jwt");
        reject("stream-ticket");

        assertThat(count("jwt")).isEqualTo(2.0);
        assertThat(count("stream-ticket")).isEqualTo(1.0);
    }

    @Test
    void stackTrace_isLoggedAtMostOncePerWindow_perFilter() throws Exception {
        assertThat(reject("jwt")).as("first failure logs the stack trace").isTrue();
        assertThat(reject("jwt")).as("second failure within the window is a one-line warn").isFalse();
        assertThat(reject("stream-ticket")).as("each filter has its own window").isTrue();

        clock.advance(AuthStoreOutageResponder.STACK_TRACE_WINDOW.minusMillis(1));
        assertThat(reject("jwt")).isFalse();

        clock.advance(Duration.ofMillis(1));
        assertThat(reject("jwt")).as("a new window logs the stack trace again").isTrue();
        assertThat(reject("jwt")).isFalse();
    }

    private boolean reject(String filter) throws Exception {
        return responder.reject(new MockHttpServletResponse(), log, filter, "user store", "CODE",
                Duration.ofSeconds(5), FAILURE);
    }

    private double count(String filter) {
        return meterRegistry.get(AuthStoreOutageResponder.METRIC).tag("filter", filter).counter().count();
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
