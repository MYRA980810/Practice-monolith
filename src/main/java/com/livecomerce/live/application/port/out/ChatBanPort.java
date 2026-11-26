package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.ChatBan;

import java.util.List;
import java.util.UUID;

/** Persistence of per-seller chat bans. */
public interface ChatBanPort {

    /**
     * Stores the ban in its own committed transaction; a no-op (no duplicate row, original kept)
     * when the seller had already banned that user.
     */
    void banIfAbsent(ChatBan ban);

    boolean isBanned(UUID sellerId, UUID userId);

    /**
     * Removes the ban; a no-op when the user is not banned.
     *
     * @return {@code true} if a ban was removed, {@code false} if there was none
     */
    boolean delete(UUID sellerId, UUID userId);

    /** The seller's bans, newest first ({@code createdAt} DESC, then id DESC), at most {@code limit}. */
    List<ChatBan> findBySeller(UUID sellerId, int limit);
}
