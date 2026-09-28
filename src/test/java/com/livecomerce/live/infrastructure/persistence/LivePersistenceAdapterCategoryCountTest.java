package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import com.livecomerce.live.domain.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivePersistenceAdapterCategoryCountTest {

    @Mock LiveJpaRepository repository;
    @InjectMocks LivePersistenceAdapter adapter;

    @Test
    void countByStatusGroupedByCategory_mapsRowsPreservingOrder() {
        var cat1 = UUID.randomUUID();
        var cat2 = UUID.randomUUID();

        when(repository.countByStatusGroupedByCategory(LiveStatus.LIVE))
                .thenReturn(List.of(new Object[]{cat1, 3L}, new Object[]{cat2, 1L}));

        var result = adapter.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(result).containsExactly(
                new CategoryLiveCount(cat1, 3L),
                new CategoryLiveCount(cat2, 1L));
    }

    @Test
    void countByStatusGroupedByCategory_returnsEmptyWhenNoRows() {
        when(repository.countByStatusGroupedByCategory(LiveStatus.LIVE)).thenReturn(List.of());

        assertThat(adapter.countByStatusGroupedByCategory(LiveStatus.LIVE)).isEmpty();
    }
}
