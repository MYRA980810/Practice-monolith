package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.StartLiveUseCase.StartLiveCommand;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartLiveServiceTest {

    @Mock LoadLivePort   loadLivePort;
    @Mock SaveLivePort   saveLivePort;
    @Mock AgoraTokenPort agoraTokenPort;
    @InjectMocks StartLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    private Live scheduledLive() {
        return Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
    }

    @Test
    void startLive_generatesTokenAndSetsStreamToken() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(agoraTokenPort.generateRtcToken(anyString(), anyString(), anyInt())).thenReturn("agora-token-abc");
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd    = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");
        var result = sut.startLive(cmd);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(result.getStreamToken()).isEqualTo("agora-token-abc");
        verify(agoraTokenPort).generateRtcToken(live.getAgoraChannelId(), "uid-42", 3600);
    }

    @Test
    void startLive_wrongSeller_throwsLiveNotOwned() {
        var live      = scheduledLive();
        var wrongSeller = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new StartLiveCommand(live.getId(), wrongSeller, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }

    @Test
    void startLive_liveNotFound_throwsLiveNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        var cmd = new StartLiveCommand(liveId, SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(LiveNotFoundException.class);
    }

    @Test
    void startLive_notScheduledStatus_throwsInvalidLiveState() {
        var live = scheduledLive();
        live.start(); // already started — token is already set
        live.setStreamToken("prev-token");
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        // NOTE: agoraTokenPort is NOT stubbed — live.start() throws before token generation

        var cmd = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(InvalidLiveStateException.class);
    }
}
