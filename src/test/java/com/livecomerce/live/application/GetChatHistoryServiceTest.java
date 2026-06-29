package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.GetChatHistoryUseCase.GetChatHistoryCommand;
import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetChatHistoryServiceTest {

    @Mock LoadChatMessagePort loadChatMessagePort;
    @InjectMocks GetChatHistoryService sut;

    private static final UUID LIVE_ID = UUID.randomUUID();

    @Test
    void getChatHistory_withBefore_callsFindByLiveIdBefore() {
        var before   = OffsetDateTime.now();
        var messages = List.of(mock(LiveChatMessage.class));
        when(loadChatMessagePort.findByLiveIdBefore(LIVE_ID, before, 20)).thenReturn(messages);

        var result = sut.getChatHistory(new GetChatHistoryCommand(LIVE_ID, before, 20));

        assertThat(result).isSameAs(messages);
        verify(loadChatMessagePort).findByLiveIdBefore(LIVE_ID, before, 20);
    }

    @Test
    void getChatHistory_withNullBefore_callsFindByLiveIdLatest() {
        var messages = List.of(mock(LiveChatMessage.class));
        when(loadChatMessagePort.findByLiveIdLatest(LIVE_ID, 30)).thenReturn(messages);

        var result = sut.getChatHistory(new GetChatHistoryCommand(LIVE_ID, null, 30));

        assertThat(result).isSameAs(messages);
        verify(loadChatMessagePort).findByLiveIdLatest(LIVE_ID, 30);
    }

    @Test
    void getChatHistory_zeroLimit_clampsToFifty() {
        when(loadChatMessagePort.findByLiveIdLatest(LIVE_ID, 50)).thenReturn(List.of());

        sut.getChatHistory(new GetChatHistoryCommand(LIVE_ID, null, 0));

        verify(loadChatMessagePort).findByLiveIdLatest(LIVE_ID, 50);
    }

    @Test
    void getChatHistory_limitAbove100_clampsToHundred() {
        when(loadChatMessagePort.findByLiveIdLatest(LIVE_ID, 100)).thenReturn(List.of());

        sut.getChatHistory(new GetChatHistoryCommand(LIVE_ID, null, 200));

        verify(loadChatMessagePort).findByLiveIdLatest(LIVE_ID, 100);
    }

    @Test
    void getChatHistory_limitAbove100_withBefore_clampsToHundred() {
        var before = OffsetDateTime.now();
        when(loadChatMessagePort.findByLiveIdBefore(LIVE_ID, before, 100)).thenReturn(List.of());

        sut.getChatHistory(new GetChatHistoryCommand(LIVE_ID, before, 150));

        verify(loadChatMessagePort).findByLiveIdBefore(LIVE_ID, before, 100);
    }
}
