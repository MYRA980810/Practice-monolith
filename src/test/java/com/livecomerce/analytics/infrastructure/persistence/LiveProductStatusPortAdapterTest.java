package com.livecomerce.analytics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adapter tests mock the JPA repository, avoiding an embedded database (matches
 * ProductSalesPortAdapterTest precedent for the same cross-module port pattern,
 * both implemented in {@code analytics} to keep {@code live} from ever depending
 * on {@code catalog} — see LiveProductReadRepository's javadoc for the cycle it avoids).
 */
@ExtendWith(MockitoExtension.class)
class LiveProductStatusPortAdapterTest {

    @Mock LiveProductReadRepository repository;

    @InjectMocks LiveProductStatusPortAdapter adapter;

    @Test
    void loadStatuses_withEmptyInput_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadStatuses(Set.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findActiveLiveRowsByProductIds(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadStatuses_pinnedInLiveLive_bothFlagsTrue() {
        var productId = UUID.randomUUID();
        when(repository.findActiveLiveRowsByProductIds(Set.of(productId))).thenReturn(List.<Object[]>of(
                new Object[]{productId, "PINNED", "LIVE"}
        ));

        var result = adapter.loadStatuses(Set.of(productId));

        assertThat(result.get(productId)).isEqualTo(new com.livecomerce.catalog.LoadLiveProductStatusPort.LiveProductBadge(true, true));
    }

    @Test
    void loadStatuses_availableButNotPinned_onlyExclusiveFlagTrue() {
        var productId = UUID.randomUUID();
        when(repository.findActiveLiveRowsByProductIds(Set.of(productId))).thenReturn(List.<Object[]>of(
                new Object[]{productId, "AVAILABLE", "LIVE"}
        ));

        var result = adapter.loadStatuses(Set.of(productId));

        assertThat(result.get(productId)).isEqualTo(new com.livecomerce.catalog.LoadLiveProductStatusPort.LiveProductBadge(false, true));
    }

    @Test
    void loadStatuses_pinnedInReconnectingLive_notPinnedNowButStillExclusive() {
        var productId = UUID.randomUUID();
        when(repository.findActiveLiveRowsByProductIds(Set.of(productId))).thenReturn(List.<Object[]>of(
                new Object[]{productId, "PINNED", "RECONNECTING"}
        ));

        var result = adapter.loadStatuses(Set.of(productId));

        assertThat(result.get(productId)).isEqualTo(new com.livecomerce.catalog.LoadLiveProductStatusPort.LiveProductBadge(false, true));
    }

    @Test
    void loadStatuses_soldStatus_excludedFromBothFlags() {
        var productId = UUID.randomUUID();
        // The repository query already filters to LIVE/RECONNECTING lives; a SOLD row can
        // still surface (a live product can sell out while its live stays LIVE) and must
        // not count toward either flag.
        when(repository.findActiveLiveRowsByProductIds(Set.of(productId))).thenReturn(List.<Object[]>of(
                new Object[]{productId, "SOLD", "LIVE"}
        ));

        var result = adapter.loadStatuses(Set.of(productId));

        assertThat(result).doesNotContainKey(productId);
    }

    @Test
    void loadStatuses_noMatchingRows_productAbsentFromResult() {
        var productId = UUID.randomUUID();
        when(repository.findActiveLiveRowsByProductIds(Set.of(productId))).thenReturn(List.of());

        var result = adapter.loadStatuses(Set.of(productId));

        assertThat(result).doesNotContainKey(productId);
    }
}
