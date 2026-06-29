package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface GetChatTokenUseCase {

    ChatTokenResult getChatToken(GetChatTokenCommand command);

    record GetChatTokenCommand(UUID liveId, UUID userId) {}

    record ChatTokenResult(String token, String channelName, String appId) {}
}
