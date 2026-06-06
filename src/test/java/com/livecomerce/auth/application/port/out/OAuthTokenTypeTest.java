package com.livecomerce.auth.application.port.out;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthTokenTypeTest {

    @Test
    void enum_hasTwoConstants() {
        assertThat(OAuthTokenType.values()).hasSize(2);
    }

    @Test
    void enum_hasFull() {
        assertThat(OAuthTokenType.FULL).isNotNull();
    }

    @Test
    void enum_hasOauthPending() {
        assertThat(OAuthTokenType.OAUTH_PENDING).isNotNull();
    }
}
