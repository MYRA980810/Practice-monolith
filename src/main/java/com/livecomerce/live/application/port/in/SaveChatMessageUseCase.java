package com.livecomerce.live.application.port.in;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SaveChatMessageUseCase {

    void saveChatMessage(SaveChatMessageCommand command);

    record SaveChatMessageCommand(
            UUID liveId,
            String agoraMsgId,
            UUID userId,
            String username,
            String content,
            OffsetDateTime sentAt
    ) {}
}
