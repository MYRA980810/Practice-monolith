package com.livecomerce.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browsers hide response headers from cross-origin JS unless they're listed in
 * Access-Control-Expose-Headers: the frontend reads {@code X-Feed-Version} from
 * GET /api/lives/active to know which live-feed events to apply.
 */
class SecurityConfigCorsTest {

    @Test
    void corsConfiguration_exposesAuthorizationAndFeedVersionHeaders() {
        var securityConfig = new SecurityConfig(null, null, null, null, null);
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:3000");

        var config = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/lives/active"));

        assertThat(config).isNotNull();
        assertThat(config.getExposedHeaders()).contains("Authorization", "X-Feed-Version");
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:3000");
    }
}
