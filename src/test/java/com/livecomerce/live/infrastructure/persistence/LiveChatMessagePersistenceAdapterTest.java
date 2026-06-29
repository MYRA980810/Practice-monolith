package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Adapter tests use Mockito to mock the JPA repository, avoiding the need for
 * an embedded database or Flyway migrations in tests.
 */
@ExtendWith(MockitoExtension.class)
class LiveChatMessagePersistenceAdapterTest {

    @Mock
    LiveChatMessageJpaRepository repository;

    @InjectMocks
    LiveChatMessagePersistenceAdapter adapter;

    @Test
    void save_delegatesToRepository() {
        var msg = mock(LiveChatMessage.class);
        when(repository.save(msg)).thenReturn(msg);

        var result = ((SaveChatMessagePort) adapter).save(msg);

        assertThat(result).isSameAs(msg);
        verify(repository).save(msg);
    }

    @Test
    void existsByAgoraMsgId_delegatesToRepository() {
        when(repository.existsByAgoraMsgId("msg-001")).thenReturn(true);

        assertThat(((SaveChatMessagePort) adapter).existsByAgoraMsgId("msg-001")).isTrue();
        verify(repository).existsByAgoraMsgId("msg-001");
    }

    @Test
    void findByLiveIdBefore_delegatesToRepository() {
        var liveId = UUID.randomUUID();
        var before = OffsetDateTime.now();
        var messages = List.of(mock(LiveChatMessage.class));
        when(repository.findByLiveIdAndSentAtBeforeOrderBySentAtDesc(liveId, before, PageRequest.of(0, 10)))
                .thenReturn(messages);

        var result = ((LoadChatMessagePort) adapter).findByLiveIdBefore(liveId, before, 10);

        assertThat(result).isSameAs(messages);
    }

    @Test
    void findByLiveIdLatest_delegatesToRepository() {
        var liveId = UUID.randomUUID();
        var messages = List.of(mock(LiveChatMessage.class));
        when(repository.findByLiveIdOrderBySentAtDesc(liveId, PageRequest.of(0, 20))).thenReturn(messages);

        var result = ((LoadChatMessagePort) adapter).findByLiveIdLatest(liveId, 20);

        assertThat(result).isSameAs(messages);
    }
}
