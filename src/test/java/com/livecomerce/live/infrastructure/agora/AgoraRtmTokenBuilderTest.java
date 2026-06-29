package com.livecomerce.live.infrastructure.agora;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgoraRtmTokenBuilderTest {

    private static final String APP_ID   = "970ca35de60c44645bbb8c4b22f1d6b9";
    private static final String APP_CERT = "5cfd2fd1755d40ecb72977518be15d3b";
    private static final String USER_ID  = "550e8400e29b41d4a716446655440000";

    @Test
    void buildTokenWithUserAccount_returnsTokenStartingWith007() {
        var token = AgoraRtmTokenBuilder.buildTokenWithUserAccount(APP_ID, APP_CERT, USER_ID, 3600);
        assertThat(token).startsWith("007");
    }

    @Test
    void buildTokenWithUserAccount_hasSubstantialLength() {
        var token = AgoraRtmTokenBuilder.buildTokenWithUserAccount(APP_ID, APP_CERT, USER_ID, 3600);
        assertThat(token.length()).isGreaterThan(100);
    }

    @Test
    void buildTokenWithUserAccount_differentUsers_produceDifferentTokens() {
        var token1 = AgoraRtmTokenBuilder.buildTokenWithUserAccount(APP_ID, APP_CERT, "user-aaa", 3600);
        var token2 = AgoraRtmTokenBuilder.buildTokenWithUserAccount(APP_ID, APP_CERT, "user-bbb", 3600);
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void buildTokenWithUserAccount_invalidAppId_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                AgoraRtmTokenBuilder.buildTokenWithUserAccount("not-a-valid-id", APP_CERT, USER_ID, 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
