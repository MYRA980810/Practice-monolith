package com.livecomerce.auth.application.port.out;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCodePayloadTest {

    @Test
    void record_constructionAndAccessors() {
        UUID userId = UUID.randomUUID();
        var payload = new OAuthCodePayload(userId, OAuthTokenType.FULL);

        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.tokenType()).isEqualTo(OAuthTokenType.FULL);
    }

    @Test
    void record_equality() {
        UUID userId = UUID.randomUUID();
        var a = new OAuthCodePayload(userId, OAuthTokenType.OAUTH_PENDING);
        var b = new OAuthCodePayload(userId, OAuthTokenType.OAUTH_PENDING);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void record_differentTokenType_notEqual() {
        UUID userId = UUID.randomUUID();
        var a = new OAuthCodePayload(userId, OAuthTokenType.FULL);
        var b = new OAuthCodePayload(userId, OAuthTokenType.OAUTH_PENDING);

        assertThat(a).isNotEqualTo(b);
    }
}
