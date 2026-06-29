package com.livecomerce.live.infrastructure.agora;

import com.livecomerce.live.application.port.out.AgoraTokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AgoraTokenAdapterRtmTest {

    private static final String APP_ID   = "970ca35de60c44645bbb8c4b22f1d6b9";
    private static final String APP_CERT = "5cfd2fd1755d40ecb72977518be15d3b";
    private static final String USER_ID  = "550e8400e29b41d4a716446655440000";

    private AgoraTokenPort buildAdapter() {
        var adapter = new AgoraTokenAdapter();
        ReflectionTestUtils.setField(adapter, "appId", APP_ID);
        ReflectionTestUtils.setField(adapter, "appCertificate", APP_CERT);
        return adapter;
    }

    @Test
    void generateRtmToken_returnsNonBlankToken() {
        var token = buildAdapter().generateRtmToken(USER_ID, 3600);
        assertThat(token).isNotBlank();
        assertThat(token).startsWith("007");
    }

    @Test
    void getAppId_returnsConfiguredAppId() {
        assertThat(buildAdapter().getAppId()).isEqualTo(APP_ID);
    }
}
