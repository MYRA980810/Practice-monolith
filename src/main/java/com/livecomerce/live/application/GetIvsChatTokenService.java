package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase;
import com.livecomerce.live.application.port.out.ChatBanPort;
import com.livecomerce.live.application.port.out.IvsChatTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues the IVS Chat token of a live. Not transactional on purpose: the live is read once
 * and no DB connection should be held while the AWS call is in flight.
 */
@Service
public class GetIvsChatTokenService implements GetIvsChatTokenUseCase {

    static final String FALLBACK_DISPLAY_NAME = ChatDisplayNames.FALLBACK;

    /** Token attribute keys: part of the wire contract with the chat clients, do not rename. */
    static final String ATTR_LIVE_ID      = "liveId";
    static final String ATTR_DISPLAY_NAME = "displayName";
    static final String ATTR_ROLE         = "role";

    /** Value of the {@value #ATTR_ROLE} attribute; the enum names are the wire values. */
    enum ChatRole { SELLER, BUYER }

    private final LoadLivePort        loadLivePort;
    private final IvsChatTokenPort    ivsChatTokenPort;
    /** Resolves any user's display name (alias, else first + last name), not only sellers. */
    private final LoadSellerNamesPort loadUserNamesPort;
    private final ChatBanPort         chatBanPort;
    private final int                 sessionDurationMinutes;

    GetIvsChatTokenService(LoadLivePort loadLivePort,
                           IvsChatTokenPort ivsChatTokenPort,
                           LoadSellerNamesPort loadUserNamesPort,
                           ChatBanPort chatBanPort,
                           @Value("${ivs.chat.session-duration-minutes:60}") int sessionDurationMinutes) {
        this.loadLivePort           = loadLivePort;
        this.ivsChatTokenPort       = ivsChatTokenPort;
        this.loadUserNamesPort      = loadUserNamesPort;
        this.chatBanPort            = chatBanPort;
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    @Override
    public IvsChatTokenResult getIvsChatToken(GetIvsChatTokenCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        if (live.getStatus() != LiveStatus.LIVE && live.getStatus() != LiveStatus.RECONNECTING) {
            throw new LiveNotLiveException(live.getId());
        }
        if (live.getChatRoomArn() == null) {
            throw new LiveChatRoomNotAvailableException(live.getId());
        }

        var role = live.getSellerId().equals(command.userId()) ? ChatRole.SELLER : ChatRole.BUYER;
        // The owner is never banned from their own chat; bans are per seller, not per live.
        // A banned user still reads the chat: the token is receive-only instead of refused.
        boolean canSend = role == ChatRole.SELLER || !chatBanPort.isBanned(live.getSellerId(), command.userId());
        var attributes = Map.of(
                ATTR_LIVE_ID,      live.getId().toString(),
                ATTR_DISPLAY_NAME, displayName(command.userId()),
                ATTR_ROLE,         role.name());

        var token = ivsChatTokenPort.createToken(
                live.getChatRoomArn(), command.userId().toString(), attributes, canSend, sessionDurationMinutes);

        return new IvsChatTokenResult(
                token.token(), token.sessionExpirationTime(), token.tokenExpirationTime(), token.region(), canSend);
    }

    /** The name is cosmetic: a failed lookup falls back instead of failing the token. */
    private String displayName(UUID userId) {
        return ChatDisplayNames.resolve(loadUserNamesPort, List.of(userId)).get(userId);
    }
}
