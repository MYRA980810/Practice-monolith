package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveChatMessage;
import jakarta.annotation.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface GetChatHistoryUseCase {

    List<LiveChatMessage> getChatHistory(GetChatHistoryCommand command);

    record GetChatHistoryCommand(UUID liveId, @Nullable OffsetDateTime before, int limit) {}
}
