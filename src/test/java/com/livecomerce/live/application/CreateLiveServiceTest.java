package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.CreateLiveUseCase.CreateLiveCommand;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateLiveServiceTest {

    @Mock  SaveLivePort saveLivePort;
    @InjectMocks CreateLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void storeContext_withNullStoreId_throwsIllegalArgument() {
        var cmd = new CreateLiveCommand(SELLER_ID, null, LiveContext.STORE, "My Live", null, null, 60);

        assertThatThrownBy(() -> sut.createLive(cmd))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(saveLivePort);
    }

    @Test
    void storeContext_withStoreId_savesAndReturnsLive() {
        var captor = ArgumentCaptor.forClass(Live.class);
        when(saveLivePort.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateLiveCommand(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        var result = sut.createLive(cmd);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.SCHEDULED);
        assertThat(result.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getContext()).isEqualTo(LiveContext.STORE);
        verify(saveLivePort).save(any(Live.class));
    }

    @Test
    void sellerProfileContext_withNullStoreId_succeeds() {
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateLiveCommand(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Profile Live", null, null, 60);
        var result = sut.createLive(cmd);

        assertThat(result.getStoreId()).isNull();
        assertThat(result.getContext()).isEqualTo(LiveContext.SELLER_PROFILE);
        assertThat(result.getStatus()).isEqualTo(LiveStatus.SCHEDULED);
    }
}
