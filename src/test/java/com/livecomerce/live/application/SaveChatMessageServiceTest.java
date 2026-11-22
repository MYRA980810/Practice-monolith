package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.SaveChatMessageUseCase.SaveChatMessageCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveChatMessageServiceTest {

    @Mock LoadLivePort       loadLivePort;
    @Mock SaveChatMessagePort saveChatMessagePort;
    @InjectMocks SaveChatMessageService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void saveChatMessage_newMessage_persists() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        var cmd  = new SaveChatMessageCommand(
                live.getId(), "msg-001", UUID.randomUUID(), "alice", "Hello!", OffsetDateTime.now());

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveChatMessagePort.existsByProviderMsgId("msg-001")).thenReturn(false);
        when(saveChatMessagePort.saveIfAbsent(any())).thenReturn(true);

        assertThatNoException().isThrownBy(() -> sut.saveChatMessage(cmd));
        verify(saveChatMessagePort).saveIfAbsent(any(LiveChatMessage.class));
    }

    @Test
    void saveChatMessage_duplicateProviderMsgId_idempotent_noException() {
        var liveId = UUID.randomUUID();
        var cmd = new SaveChatMessageCommand(
                liveId, "msg-dup", UUID.randomUUID(), "alice", "Hello!", OffsetDateTime.now());

        when(saveChatMessagePort.existsByProviderMsgId("msg-dup")).thenReturn(true);

        assertThatNoException().isThrownBy(() -> sut.saveChatMessage(cmd));
        verifyNoInteractions(loadLivePort);
        verify(saveChatMessagePort, never()).saveIfAbsent(any());
    }

    @Test
    void saveChatMessage_concurrentDuplicateLosesInsertRace_noException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        var cmd  = new SaveChatMessageCommand(
                live.getId(), "msg-race", UUID.randomUUID(), "alice", "Hello!", OffsetDateTime.now());

        when(saveChatMessagePort.existsByProviderMsgId("msg-race")).thenReturn(false);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveChatMessagePort.saveIfAbsent(any())).thenReturn(false); // another consumer inserted it first

        assertThatNoException().isThrownBy(() -> sut.saveChatMessage(cmd));
    }

    @Test
    void saveChatMessage_liveNotFound_throwsLiveNotFoundException() {
        var liveId = UUID.randomUUID();
        var cmd = new SaveChatMessageCommand(
                liveId, "msg-002", UUID.randomUUID(), "bob", "Hi!", OffsetDateTime.now());

        when(saveChatMessagePort.existsByProviderMsgId("msg-002")).thenReturn(false);
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.saveChatMessage(cmd))
                .isInstanceOf(LiveNotFoundException.class);
    }
}
