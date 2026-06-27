package com.livecomerce.live.infrastructure.agora;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AgoraRtcTokenBuilderTest {

    // Agora App ID and Certificate are 32-char lowercase hex strings (no dashes)
    private static final String APP_ID   = "970ca35de60c44645bbb8c4b22f1d6b9";
    private static final String APP_CERT = "5cfd2fd1755d40ecb72977518be15d3b";
    private static final String CHANNEL  = "live-channel-abc";
    private static final String UID      = "42";

    @Test
    void buildToken_returnsTokenStartingWith007() {
        var token = AgoraRtcTokenBuilder.buildToken(APP_ID, APP_CERT, CHANNEL, UID, 3600);

        assertThat(token).startsWith("007");
    }

    @Test
    void buildToken_hasSubstantialLength() {
        var token = AgoraRtcTokenBuilder.buildToken(APP_ID, APP_CERT, CHANNEL, UID, 3600);

        assertThat(token.length()).isGreaterThan(100);
    }

    @Test
    void buildToken_differentChannels_produceDifferentTokens() {
        var token1 = AgoraRtcTokenBuilder.buildToken(APP_ID, APP_CERT, "channel-A", UID, 3600);
        var token2 = AgoraRtcTokenBuilder.buildToken(APP_ID, APP_CERT, "channel-B", UID, 3600);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void buildToken_invalidAppId_throwsIllegalArgument() {
        // Agora App IDs must be 32-char hex — dashes or wrong length fail validation
        assertThatThrownBy(() ->
                AgoraRtcTokenBuilder.buildToken("not-a-valid-agora-id", APP_CERT, CHANNEL, UID, 3600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32-char hex");
    }
}
