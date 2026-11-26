package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase;
import com.livecomerce.live.application.port.out.ChatBanPort;
import com.livecomerce.live.application.port.out.ChatModerationPort;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.ChatBan;
import com.livecomerce.live.domain.ChatRoomNotFoundException;
import com.livecomerce.live.domain.ChatSelfBanException;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chat moderation by the owner of the live. Not transactional on purpose: every DB write commits
 * in its own short transaction (in the adapters) and no DB connection is held during IVS calls.
 * <ul>
 *   <li>Ban: the ban is committed first, then the user is disconnected. If IVS fails the ban stays
 *       and the caller gets 503; re-banning is idempotent and retries the disconnect.</li>
 *   <li>Unban: the ban is deleted first, then the room is told (best-effort, never fails the unban).</li>
 *   <li>Delete message: IVS first, then the soft delete, so an IVS failure leaves the DB untouched.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ModerateLiveChatService implements ModerateLiveChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(ModerateLiveChatService.class);

    /** Safety cap of the ban list (newest first); there is no pagination. */
    static final int MAX_LISTED_BANS = 1000;

    private final LoadLivePort        loadLivePort;
    private final ChatBanPort         chatBanPort;
    private final ChatModerationPort  chatModerationPort;
    private final SaveChatMessagePort saveChatMessagePort;
    /** Resolves any user's display name (alias, else first + last name), not only sellers. */
    private final LoadSellerNamesPort loadUserNamesPort;
    private final LiveRoomEventPort   liveRoomEventPort;

    /**
     * Disconnect + token check is enough: IVS chat tokens are single-use and a closed connection needs
     * a new token, which our token endpoint issues receive-only (no SEND_MESSAGE) to a banned user, so
     * they keep reading but cannot send. The only residual window is a sending token issued before the
     * ban and not used yet, until its tokenExpirationTime.
     */
    @Override
    public void banUser(BanChatUserCommand command) {
        var live = loadOwnedLive(command.liveId(), command.sellerId());
        if (live.getSellerId().equals(command.userId())) {
            throw new ChatSelfBanException();
        }

        // Bans are per seller: the user stays banned in every future live of this seller.
        chatBanPort.banIfAbsent(ChatBan.create(live.getSellerId(), command.userId(), command.reason(), command.sellerId()));

        // Intentionally unconditional (also when the ban already existed): a re-ban retries a failed disconnect.
        // Only an on-air live has a room to disconnect from: the room is deleted when the live ends.
        if (live.getChatRoomArn() != null && isOnAir(live)) {
            chatModerationPort.disconnectUser(live.getChatRoomArn(), command.userId().toString(), command.reason());
        }
    }

    /**
     * Idempotent. When a ban was actually removed and the live is on air with a chat room, the room is
     * told {@code chat-user-unbanned} so the unbanned client fetches a token that can send again.
     */
    @Override
    public void unbanUser(UnbanChatUserCommand command) {
        var live = loadOwnedLive(command.liveId(), command.sellerId());
        boolean removed = chatBanPort.delete(live.getSellerId(), command.userId());

        if (removed && live.getChatRoomArn() != null && isOnAir(live)) {
            afterCommit(() -> announceUnban(live, command.userId()));
        }
    }

    /** Best-effort: a failed announcement never fails the committed unban; the client recovers on reload. */
    private void announceUnban(Live live, UUID userId) {
        try {
            liveRoomEventPort.publish(live, new RoomEvent.ChatUserUnbanned(userId));
        } catch (Exception e) {
            log.warn("chat-user-unbanned room event failed: liveId={}, error={}", live.getId(), e.getMessage());
        }
    }

    /**
     * Same rule as {@link LiveRoomCloser}: never announce a change a caller's transaction may still roll
     * back. This service is not transactional, so today the ban deletion has already committed and the
     * action runs immediately.
     */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    @Override
    public void deleteMessage(DeleteChatMessageCommand command) {
        var live = loadOwnedLive(command.liveId(), command.sellerId());
        // DeleteMessage needs the room, which only exists while the live is on air.
        if (!isOnAir(live)) {
            throw new LiveNotLiveException(live.getId());
        }
        if (live.getChatRoomArn() == null) {
            throw new LiveChatRoomNotAvailableException(live.getId());
        }

        try {
            chatModerationPort.deleteMessage(live.getChatRoomArn(), command.messageId());
        } catch (ChatRoomNotFoundException e) {
            // The live ended between the status check and the IVS call: same answer as a live without room.
            throw new LiveChatRoomNotAvailableException(live.getId());
        }

        // Scoped to this live: a message id of another live is never marked. 0 rows is fine,
        // IVS messages are not persisted yet.
        saveChatMessagePort.markDeleted(live.getId(), command.messageId(), OffsetDateTime.now());
    }

    /**
     * Like unban, works for any live status and chat provider: bans are per seller and the live
     * only proves ownership. Names are resolved in one batch with the same rules as the chat token.
     */
    @Override
    public List<BannedChatUser> listBans(ListChatBansQuery query) {
        var live = loadOwnedLive(query.liveId(), query.sellerId());
        var bans = chatBanPort.findBySeller(live.getSellerId(), MAX_LISTED_BANS);
        if (bans.isEmpty()) {
            return List.of();
        }
        var names = ChatDisplayNames.resolve(loadUserNamesPort, bans.stream().map(ChatBan::getUserId).toList());
        return bans.stream()
                .map(ban -> new BannedChatUser(ban.getUserId(), names.get(ban.getUserId()), ban.getCreatedAt().toInstant()))
                .toList();
    }

    private Live loadOwnedLive(UUID liveId, UUID sellerId) {
        var live = loadLivePort.loadById(liveId)
                .orElseThrow(() -> new LiveNotFoundException(liveId));
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
        return live;
    }

    private static boolean isOnAir(Live live) {
        return live.getStatus() == LiveStatus.LIVE || live.getStatus() == LiveStatus.RECONNECTING;
    }
}
