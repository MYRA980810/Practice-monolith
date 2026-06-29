package com.livecomerce.live.infrastructure.agora;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgoraAccessTokenRtmTest {

    private static final String APP_ID   = "970ca35de60c44645bbb8c4b22f1d6b9";
    private static final String APP_CERT = "5cfd2fd1755d40ecb72977518be15d3b";
    private static final String USER_ID  = "550e8400e29b41d4a716446655440000";

    @Test
    void serviceRtm_type_isTwo() {
        var service = new AgoraAccessToken.ServiceRtm(USER_ID);
        assertThat(service.getServiceType()).isEqualTo((short) 2);
    }

    @Test
    void serviceRtm_build_returnsNonBlankTokenStartingWith007() throws Exception {
        var token = new AgoraAccessToken(APP_ID, APP_CERT, 3600);
        var service = new AgoraAccessToken.ServiceRtm(USER_ID);
        service.addPrivilege(AgoraAccessToken.PrivilegeRtm.PRIVILEGE_LOGIN, 3600);
        token.addService(service);

        var built = token.build();
        assertThat(built).isNotBlank();
        assertThat(built).startsWith("007");
    }

    @Test
    void privilegeRtm_loginValue_isOne() {
        assertThat(AgoraAccessToken.PrivilegeRtm.PRIVILEGE_LOGIN.intValue).isEqualTo((short) 1);
    }
}
