package com.livecomerce.live.infrastructure.scheduler;

import com.livecomerce.live.application.EndLiveService;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleLiveReconciliationJobTest {

    @Mock LoadLivePort   loadLivePort;
    @Mock EndLiveService endLiveService;
    @InjectMocks StaleLiveReconciliationJob sut;

    private Live liveLive() {
        var live = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    void closeStaleLives_endsEachStaleLive() {
        var live1 = liveLive();
        var live2 = liveLive();
        when(loadLivePort.loadStaleLive(any())).thenReturn(List.of(live1, live2));

        sut.closeStaleLives();

        verify(endLiveService).endStaleLive(live1);
        verify(endLiveService).endStaleLive(live2);
    }

    @Test
    void closeStaleLives_noneStale_doesNothing() {
        when(loadLivePort.loadStaleLive(any())).thenReturn(List.of());

        sut.closeStaleLives();

        verifyNoInteractions(endLiveService);
    }

    @Test
    void closeStaleLives_usesGracePeriodConfiguredCutoff() throws Exception {
        var field = StaleLiveReconciliationJob.class.getDeclaredField("gracePeriodSeconds");
        field.setAccessible(true);
        field.set(sut, 180);
        when(loadLivePort.loadStaleLive(any())).thenReturn(List.of());

        var before = Instant.now().minusSeconds(180);
        sut.closeStaleLives();
        var after = Instant.now().minusSeconds(180);

        var captor = ArgumentCaptor.forClass(Instant.class);
        verify(loadLivePort).loadStaleLive(captor.capture());
        assertThat(captor.getValue()).isBetween(before, after);
    }
}
