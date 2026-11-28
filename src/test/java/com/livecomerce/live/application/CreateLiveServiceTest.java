package com.livecomerce.live.application;

import com.livecomerce.live.CategoryLookupPort;
import com.livecomerce.live.application.port.in.CreateLiveUseCase.CreateLiveCommand;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.CategoryNotAvailableException;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock  CategoryLookupPort categoryLookupPort;
    @InjectMocks CreateLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void createLive_savesScheduledLiveOwnedByTheSeller() {
        when(categoryLookupPort.isActive(CATEGORY_ID)).thenReturn(true);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateLiveCommand(SELLER_ID, "My Live", null, null, 60, CATEGORY_ID);
        var result = sut.createLive(cmd);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.SCHEDULED);
        assertThat(result.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(result.getTitle()).isEqualTo("My Live");
        verify(saveLivePort).save(any(Live.class));
    }

    // ── Category ──────────────────────────────────────────────────────────────

    @Test
    void withActiveCategory_persistsCategoryOnLive() {
        when(categoryLookupPort.isActive(CATEGORY_ID)).thenReturn(true);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateLiveCommand(SELLER_ID, "Profile Live", null, null, 60, CATEGORY_ID);
        var result = sut.createLive(cmd);

        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        verify(saveLivePort).save(any(Live.class));
    }

    @Test
    void withMissingOrInactiveCategory_throwsCategoryNotAvailable() {
        when(categoryLookupPort.isActive(CATEGORY_ID)).thenReturn(false);

        var cmd = new CreateLiveCommand(SELLER_ID, "Profile Live", null, null, 60, CATEGORY_ID);

        assertThatThrownBy(() -> sut.createLive(cmd))
                .isInstanceOf(CategoryNotAvailableException.class);
        verifyNoInteractions(saveLivePort);
    }
}
