package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivePersistenceAdapterLoadStaleLiveTest {

    @Mock LiveJpaRepository repository;
    @InjectMocks LivePersistenceAdapter adapter;

    @Test
    void loadStaleLive_delegatesToRepositoryWithLiveStatus() {
        var live   = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Test", null, null, 60);
        var cutoff = Instant.now();

        when(repository.findByStatusAndStreamEndedAtLessThanEqual(LiveStatus.LIVE, cutoff))
                .thenReturn(List.of(live));

        var result = adapter.loadStaleLive(cutoff);

        assertThat(result).containsExactly(live);
    }
}
