package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivePersistenceAdapterLoadByChannelTest {

    @Mock LiveJpaRepository repository;
    @InjectMocks LivePersistenceAdapter adapter;

    @Test
    void loadByAgoraChannelId_delegatesToRepository() {
        var live = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Test", null, null, 60);
        String channelId = live.getAgoraChannelId();

        when(repository.findByAgoraChannelId(channelId)).thenReturn(Optional.of(live));

        var result = adapter.loadByAgoraChannelId(channelId);

        assertThat(result).isPresent().contains(live);
        verify(repository).findByAgoraChannelId(channelId);
    }

    @Test
    void loadByAgoraChannelId_unknownChannel_returnsEmpty() {
        when(repository.findByAgoraChannelId("unknown-channel")).thenReturn(Optional.empty());

        var result = adapter.loadByAgoraChannelId("unknown-channel");

        assertThat(result).isEmpty();
    }
}
