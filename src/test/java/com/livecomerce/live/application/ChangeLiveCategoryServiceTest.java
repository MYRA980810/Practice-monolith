package com.livecomerce.live.application;

import com.livecomerce.live.CategoryLookupPort;
import com.livecomerce.live.LiveCategoryChangedEvent;
import com.livecomerce.live.application.port.in.ChangeLiveCategoryUseCase.ChangeLiveCategoryCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeLiveCategoryServiceTest {

    @Mock LoadLivePort       loadLivePort;
    @Mock SaveLivePort       saveLivePort;
    @Mock CategoryLookupPort categoryLookupPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ChangeLiveCategoryService sut;

    private static final UUID SELLER_ID       = UUID.randomUUID();
    private static final UUID OLD_CATEGORY_ID = UUID.randomUUID();
    private static final UUID NEW_CATEGORY_ID = UUID.randomUUID();

    private static Live scheduledLive() {
        return Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "My Live", null, null, 60, OLD_CATEGORY_ID);
    }

    @Test
    void changeCategory_withActiveCategory_updatesAndSavesLive() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(categoryLookupPort.isActive(NEW_CATEGORY_ID)).thenReturn(true);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), SELLER_ID, NEW_CATEGORY_ID));

        assertThat(result.getCategoryId()).isEqualTo(NEW_CATEGORY_ID);
        verify(saveLivePort).save(live);
    }

    @Test
    void changeCategory_liveNotFound_throwsLiveNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.changeCategory(new ChangeLiveCategoryCommand(liveId, SELLER_ID, NEW_CATEGORY_ID)))
                .isInstanceOf(LiveNotFoundException.class);
        verifyNoInteractions(categoryLookupPort, saveLivePort);
    }

    @Test
    void changeCategory_wrongSeller_throwsLiveNotOwned() {
        var live        = scheduledLive();
        var wrongSeller = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), wrongSeller, NEW_CATEGORY_ID)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
        assertThat(live.getCategoryId()).isEqualTo(OLD_CATEGORY_ID);
        verifyNoInteractions(categoryLookupPort, saveLivePort);
    }

    @Test
    void changeCategory_withMissingOrInactiveCategory_throwsCategoryNotAvailable() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(categoryLookupPort.isActive(NEW_CATEGORY_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), SELLER_ID, NEW_CATEGORY_ID)))
                .isInstanceOf(CategoryNotAvailableException.class);
        assertThat(live.getCategoryId()).isEqualTo(OLD_CATEGORY_ID);
        verifyNoInteractions(saveLivePort);
    }

    @Test
    void changeCategory_onCancelledLive_throwsInvalidLiveStateAndDoesNotSave() {
        var live = scheduledLive();
        live.cancel();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(categoryLookupPort.isActive(NEW_CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), SELLER_ID, NEW_CATEGORY_ID)))
                .isInstanceOf(InvalidLiveStateException.class);
        verifyNoInteractions(saveLivePort);
    }

    @Test
    void changeCategory_onLiveLive_publishesCategoryChangedEventForTheFeed() {
        var live = scheduledLive();
        live.start();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(categoryLookupPort.isActive(NEW_CATEGORY_ID)).thenReturn(true);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), SELLER_ID, NEW_CATEGORY_ID));

        verify(eventPublisher).publishEvent(new LiveCategoryChangedEvent(live.getId(), NEW_CATEGORY_ID));
    }

    @Test
    void changeCategory_onScheduledLive_publishesNothing() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(categoryLookupPort.isActive(NEW_CATEGORY_ID)).thenReturn(true);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.changeCategory(new ChangeLiveCategoryCommand(live.getId(), SELLER_ID, NEW_CATEGORY_ID));

        verifyNoInteractions(eventPublisher);
    }
}
