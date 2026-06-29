package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.GetChatTokenUseCase.GetChatTokenCommand;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetChatTokenServiceTest {

    @Mock LoadLivePort     loadLivePort;
    @Mock AgoraTokenPort   agoraTokenPort;
    @InjectMocks GetChatTokenService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    @Test
    void getChatToken_activeLive_returnsToken() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        live.start();

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(agoraTokenPort.generateRtmToken(USER_ID.toString(), 3600)).thenReturn("007token");
        when(agoraTokenPort.getAppId()).thenReturn("test-app-id");

        var cmd    = new GetChatTokenCommand(live.getId(), USER_ID);
        var result = sut.getChatToken(cmd);

        assertThat(result.token()).isEqualTo("007token");
        assertThat(result.channelName()).isEqualTo("live-chat:" + live.getId());
        assertThat(result.appId()).isEqualTo("test-app-id");
    }

    @Test
    void getChatToken_liveNotFound_throwsLiveNotFoundException() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getChatToken(new GetChatTokenCommand(liveId, USER_ID)))
                .isInstanceOf(LiveNotFoundException.class);
    }

    @Test
    void getChatToken_endedLive_throwsLiveNotLiveException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        live.start();
        live.end();

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.getChatToken(new GetChatTokenCommand(live.getId(), USER_ID)))
                .isInstanceOf(LiveNotLiveException.class);
    }

    @Test
    void getChatToken_scheduledLive_throwsLiveNotLiveException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        // status is SCHEDULED by default

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.getChatToken(new GetChatTokenCommand(live.getId(), USER_ID)))
                .isInstanceOf(LiveNotLiveException.class);
    }
}
