package com.livecomerce.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browsers hide response headers from cross-origin JS unless they're listed in
 * Access-Control-Expose-Headers: the frontend reads {@code X-Feed-Version} from
 * GET /api/lives/active to know which live-feed events to apply.
 * The SSE stream has its own, narrower configuration: the browser opens it directly with a
 * single-use {@code X-Stream-Ticket} instead of a Bearer token.
 */
class SecurityConfigCorsTest {

    private static final String STREAM = "/api/lives/feed/stream";

    private static CorsConfiguration configFor(String path, String feedStreamOrigins) {
        var securityConfig = new SecurityConfig(null, null, null, null, null, null);
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(securityConfig, "feedStreamAllowedOriginPatterns", feedStreamOrigins);
        return securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", path));
    }

    @Test
    void corsConfiguration_exposesAuthorizationAndFeedVersionHeaders() {
        var config = configFor("/api/lives/active", "https://livecomerce.app");

        assertThat(config).isNotNull();
        assertThat(config.getExposedHeaders()).contains("Authorization", "X-Feed-Version");
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void globalConfiguration_doesNotAllowTheStreamTicketHeader() {
        var config = configFor("/api/lives/active", "https://livecomerce.app");

        assertThat(config).isNotNull();
        assertThat(config.getAllowedHeaders()).doesNotContain("X-Stream-Ticket");
    }

    /** The SSE client sends the ticket and, on reconnect, its last seq in Last-Event-ID (CORS-preflighted). */
    @Test
    void streamConfiguration_allowsTicketAndLastEventIdOnGet_cachedFor600s_withoutCredentials() {
        var config = configFor(STREAM, "https://livecomerce.app");

        assertThat(config).isNotNull();
        assertThat(config.checkHeaders(List.of("X-Stream-Ticket", "Last-Event-ID")))
                .containsExactlyInAnyOrder("X-Stream-Ticket", "Last-Event-ID");
        assertThat(config.getAllowedMethods()).containsExactly("GET");
        assertThat(config.getMaxAge()).isEqualTo(600L);
        assertThat(config.getAllowCredentials()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    void streamConfiguration_matchesConfiguredOriginsAndVercelPreviewPatterns() {
        var config = configFor(STREAM, " https://livecomerce.app , https://livecomerce-web-*-acme.vercel.app ");

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin("https://livecomerce.app")).isEqualTo("https://livecomerce.app");
        assertThat(config.checkOrigin("https://livecomerce-web-abc123-acme.vercel.app"))
                .isEqualTo("https://livecomerce-web-abc123-acme.vercel.app");
        assertThat(config.checkOrigin("https://livecomerce-web-abc123-other.vercel.app")).isNull();
        assertThat(config.checkOrigin("http://localhost:3000")).isNull();
    }

    @Test
    void streamConfiguration_withNoConfiguredOrigins_allowsNoCrossOriginRequest() {
        var config = configFor(STREAM, "");

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin("http://localhost:3000")).isNull();
        assertThat(config.checkOrigin("https://livecomerce.app")).isNull();
    }
}
