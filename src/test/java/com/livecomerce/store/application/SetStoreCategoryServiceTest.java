package com.livecomerce.store.application;

import com.livecomerce.store.StoreCategoryPort;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetStoreCategoryServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;
    @Mock StoreCategoryPort storeCategoryPort;
    @InjectMocks SetStoreCategoryService sut;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Store buildStore() {
        return Store.create(USER_ID, "Test Store", "test-store", null, null);
    }

    @Test
    void setCategory_withActiveCategory_setsOverrideAndSaves() {
        var store = buildStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(storeCategoryPort.isActive(CATEGORY_ID)).thenReturn(true);
        when(saveStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.setCategory(USER_ID, CATEGORY_ID);

        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        verify(saveStorePort).save(store);
    }

    @Test
    void setCategory_withNull_clearsOverrideWithoutValidating() {
        var store = buildStore();
        store.changeCategory(CATEGORY_ID);
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(saveStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.setCategory(USER_ID, null);

        assertThat(result.getCategoryId()).isNull();
        verifyNoInteractions(storeCategoryPort);
    }

    @Test
    void setCategory_withInactiveOrUnknownCategory_throwsUnprocessable() {
        var store = buildStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(storeCategoryPort.isActive(CATEGORY_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.setCategory(USER_ID, CATEGORY_ID))
                .isInstanceOf(InvalidStoreCategoryException.class)
                .satisfies(e -> assertThat(((InvalidStoreCategoryException) e).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThat(store.getCategoryId()).isNull();
        verify(saveStorePort, never()).save(any());
    }

    @Test
    void setCategory_whenStoreMissing_throwsStoreNotFound() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.setCategory(USER_ID, CATEGORY_ID))
                .isInstanceOf(StoreNotFoundException.class);

        verify(saveStorePort, never()).save(any());
    }
}
