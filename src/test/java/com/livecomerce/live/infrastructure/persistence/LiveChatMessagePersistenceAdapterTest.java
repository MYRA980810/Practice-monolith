package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.Live;
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
    void saveIfAbsent_insertsThroughOnConflictQuery_andReportsWhetherARowWasInserted() {
        var live = Live.create(UUID.randomUUID(), "Live", null, null, 60);
        var msg  = LiveChatMessage.create(live, "msg-001", UUID.randomUUID(), "alice", "hi", OffsetDateTime.now());
        when(repository.insertIfAbsent(msg.getId(), live.getId(), "msg-001", msg.getUserId(),
                "alice", "hi", msg.getSentAt())).thenReturn(1, 0);

        assertThat(((SaveChatMessagePort) adapter).saveIfAbsent(msg)).isTrue();
        assertThat(((SaveChatMessagePort) adapter).saveIfAbsent(msg)).isFalse();
    }

    @Test
    void insertIfAbsent_targetsTheExistingAgoraColumn() throws NoSuchMethodException {
        var method = LiveChatMessageJpaRepository.class.getDeclaredMethod("insertIfAbsent",
                UUID.class, UUID.class, String.class, UUID.class, String.class, String.class, OffsetDateTime.class);
        var sql = method.getAnnotation(org.springframework.data.jpa.repository.Query.class).value();

        assertThat(sql).contains("ON CONFLICT (agora_msg_id) DO NOTHING").doesNotContain("provider_msg_id");
    }

    @Test
    void existsByProviderMsgId_delegatesToRepository() {
        when(repository.existsByProviderMsgId("msg-001")).thenReturn(true);

        assertThat(((SaveChatMessagePort) adapter).existsByProviderMsgId("msg-001")).isTrue();
        verify(repository).existsByProviderMsgId("msg-001");
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
