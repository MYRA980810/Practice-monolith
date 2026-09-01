package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.AgoraChatTokenPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLiveFeedTokenServiceTest {

    @Mock AgoraChatTokenPort agoraTokenPort;
    @InjectMocks GetLiveFeedTokenService sut;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getFeedToken_returnsTokenForFeedChannel() {
        when(agoraTokenPort.generateRtmToken(USER_ID.toString(), 3600)).thenReturn("007token");
        when(agoraTokenPort.getAppId()).thenReturn("test-app-id");

        var result = sut.getFeedToken(USER_ID);

        assertThat(result.token()).isEqualTo("007token");
        assertThat(result.channelName()).isEqualTo("lives-feed");
        assertThat(result.appId()).isEqualTo("test-app-id");
    }
}
