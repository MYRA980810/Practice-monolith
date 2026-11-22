package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.ChatBan;

import java.util.UUID;

/** Persistence of per-seller chat bans. */
public interface ChatBanPort {

    /**
     * Stores the ban in its own committed transaction; a no-op (no duplicate row, original kept)
     * when the seller had already banned that user.
     */
    void banIfAbsent(ChatBan ban);

    boolean isBanned(UUID sellerId, UUID userId);

    /** Removes the ban; a no-op when the user is not banned. */
    void delete(UUID sellerId, UUID userId);
}
