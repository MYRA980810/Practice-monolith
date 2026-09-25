package com.livecomerce.store.domain;

import com.livecomerce.store.application.StoreCannotBeReactivatedException;
import com.livecomerce.store.application.StoreCannotBeReopenedException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private Store activeStore() {
        return Store.create(USER_ID, "Test Store", "test-store", null, null);
    }

    private Store suspendedStore() {
        var store = Store.create(USER_ID, "Test Store", "test-store", null, null);
        store.suspend(SuspensionReason.BILLING);
        return store;
    }

    @Test
    void reactivate_whenNotSuspended_setsActiveTrue() {
        var store = activeStore();
        store.deactivate();
        assertThat(store.isActive()).isFalse();

        store.reactivate();

        assertThat(store.isActive()).isTrue();
    }

    @Test
    void reactivate_whenSuspended_throwsStoreCannotBeReactivatedException() {
        var store = suspendedStore();

        assertThatThrownBy(store::reactivate)
                .isInstanceOf(StoreCannotBeReactivatedException.class);

        assertThat(store.isActive()).isTrue();
    }

    @Test
    void reactivate_whenAlreadyActive_isIdempotent() {
        var store = activeStore();
        assertThat(store.isActive()).isTrue();

        store.reactivate();

        assertThat(store.isActive()).isTrue();
    }

    @Test
    void deactivate_setsActiveFalse() {
        var store = activeStore();

        store.deactivate();

        assertThat(store.isActive()).isFalse();
        assertThat(store.isSuspended()).isFalse();
    }

    // --- closeTemporarily / reopen ---

    @Test
    void create_isNotTemporarilyClosed_byDefault() {
        var store = activeStore();
        assertThat(store.isTemporarilyClosed()).isFalse();
    }

    @Test
    void closeTemporarily_whenActive_setsTemporarilyClosedTrue() {
        var store = activeStore();
        store.closeTemporarily();
        assertThat(store.isTemporarilyClosed()).isTrue();
    }

    @Test
    void closeTemporarily_whenAlreadyClosed_isIdempotent() {
        var store = activeStore();
        store.closeTemporarily();
        store.closeTemporarily();
        assertThat(store.isTemporarilyClosed()).isTrue();
    }

    @Test
    void closeTemporarily_whenInactive_throwsIllegalStateException() {
        var store = activeStore();
        store.deactivate();
        assertThatThrownBy(store::closeTemporarily)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reopen_whenTemporarilyClosed_setsTemporarilyClosedFalse() {
        var store = activeStore();
        store.closeTemporarily();
        store.reopen();
        assertThat(store.isTemporarilyClosed()).isFalse();
    }

    @Test
    void reopen_whenNotClosed_isIdempotent() {
        var store = activeStore();
        store.reopen();
        assertThat(store.isTemporarilyClosed()).isFalse();
    }

    @Test
    void reopen_whenSuspended_throwsStoreCannotBeReopenedException() {
        var store = suspendedStore();
        store.closeTemporarily();
        assertThatThrownBy(store::reopen)
                .isInstanceOf(StoreCannotBeReopenedException.class);
    }

    @Test
    void create_hasNoCategoryOverride() {
        assertThat(activeStore().getCategoryId()).isNull();
    }

    @Test
    void changeCategory_setsOverrideAndBumpsUpdatedAt() {
        var store = activeStore();
        var before = OffsetDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(store, "updatedAt", before);
        var categoryId = UUID.randomUUID();

        store.changeCategory(categoryId);

        assertThat(store.getCategoryId()).isEqualTo(categoryId);
        assertThat(store.getUpdatedAt()).isAfter(before);
    }

    @Test
    void changeCategory_withNull_clearsOverrideAndBumpsUpdatedAt() {
        var store = activeStore();
        store.changeCategory(UUID.randomUUID());
        var before = OffsetDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(store, "updatedAt", before);

        store.changeCategory(null);

        assertThat(store.getCategoryId()).isNull();
        assertThat(store.getUpdatedAt()).isAfter(before);
    }
}
