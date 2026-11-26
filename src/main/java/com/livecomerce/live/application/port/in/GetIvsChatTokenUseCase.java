package com.livecomerce.live.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** Issues an Amazon IVS Chat token so a user (seller or buyer) can join the chat room of a live. */
public interface GetIvsChatTokenUseCase {

    IvsChatTokenResult getIvsChatToken(GetIvsChatTokenCommand command);

    record GetIvsChatTokenCommand(UUID liveId, UUID userId) {}

    /** {@code canSend} is false for a user banned by the live's seller: the token is receive-only. */
    record IvsChatTokenResult(String token, Instant sessionExpirationTime,
                              Instant tokenExpirationTime, String region, boolean canSend) {}
}
