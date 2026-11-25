package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BanChatUserCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BannedChatUser;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.ListChatBansQuery;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.DeleteChatMessageCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.UnbanChatUserCommand;
import com.livecomerce.live.application.port.out.ChatBanPort;
import com.livecomerce.live.application.port.out.ChatModerationPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.ChatBan;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.ChatRoomNotFoundException;
import com.livecomerce.live.domain.ChatSelfBanException;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerateLiveChatServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock ChatBanPort         chatBanPort;
    @Mock ChatModerationPort  chatModerationPort;
    @Mock SaveChatMessagePort saveChatMessagePort;
    @Mock LoadSellerNamesPort loadUserNamesPort;

    @InjectMocks ModerateLiveChatService sut;

    private static final UUID   SELLER_ID = UUID.randomUUID();
    private static final UUID   OTHER_SELLER_ID = UUID.randomUUID();
    private static final UUID   BUYER_ID  = UUID.randomUUID();
    private static final String ROOM_ARN  = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final String MSG_ID    = "ivs-msg-1";

    private static Live activeLiveWithRoom() {
        var live = Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60);
        live.start();
        live.setChatRoom(ROOM_ARN);
        return live;
    }

    private static Live activeLiveWithoutRoom() {
        var live = Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60);
        live.start();
        return live;
    }

    private Live given(Live live) {
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        return live;
    }

    // ── ban ──────────────────────────────────────────────────────────────────

    @Test
    void ban_owner_persistsBanForTheSeller_thenDisconnectsUserFromRoom() {
        var live = given(activeLiveWithRoom());

        sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID, "spam"));

        var captor = ArgumentCaptor.forClass(ChatBan.class);
        InOrder order = inOrder(chatBanPort, chatModerationPort);
        order.verify(chatBanPort).banIfAbsent(captor.capture());
        order.verify(chatModerationPort).disconnectUser(ROOM_ARN, BUYER_ID.toString(), "spam");
        var ban = captor.getValue();
        assertThat(ban.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(ban.getUserId()).isEqualTo(BUYER_ID);
        assertThat(ban.getReason()).isEqualTo("spam");
        assertThat(ban.getBannedBy()).isEqualTo(SELLER_ID);
    }

    @Test
    void reBan_stillDisconnects_soItRetriesAFailedDisconnect() {
        var live = given(activeLiveWithRoom());

        sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID, null));

        verify(chatModerationPort).disconnectUser(ROOM_ARN, BUYER_ID.toString(), null);
    }

    @Test
    void ban_nonOwner_throwsNotOwned_andTouchesNothing() {
        var live = given(activeLiveWithRoom());

        assertThatThrownBy(() -> sut.banUser(new BanChatUserCommand(live.getId(), OTHER_SELLER_ID, BUYER_ID, null)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);

        verifyNoInteractions(chatBanPort, chatModerationPort);
    }

    @Test
    void ban_self_throwsSelfBan_andTouchesNothing() {
        var live = given(activeLiveWithRoom());

        assertThatThrownBy(() -> sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, SELLER_ID, null)))
                .isInstanceOf(ChatSelfBanException.class);

        verifyNoInteractions(chatBanPort, chatModerationPort);
    }

    @Test
    void ban_unknownLive_throwsNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.banUser(new BanChatUserCommand(liveId, SELLER_ID, BUYER_ID, null)))
                .isInstanceOf(LiveNotFoundException.class);

        verifyNoInteractions(chatBanPort, chatModerationPort);
    }

    @Test
    void ban_liveWithoutRoom_persistsBan_withoutIvsCall() {
        var live = given(activeLiveWithoutRoom());

        sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID, null));

        verify(chatBanPort).banIfAbsent(any());
        verifyNoInteractions(chatModerationPort);
    }

    @Test
    void ban_endedLive_persistsBan_withoutIvsCall_becauseTheRoomIsGone() {
        var live = activeLiveWithRoom();
        live.end();
        given(live);

        sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID, null));

        verify(chatBanPort).banIfAbsent(any());
        verifyNoInteractions(chatModerationPort);
    }

    @Test
    void ban_disconnectFails_banStaysPersisted_andProviderErrorPropagates() {
        var live = given(activeLiveWithRoom());
        doThrow(new ChatProviderUnavailableException("down", new RuntimeException()))
                .when(chatModerationPort).disconnectUser(anyString(), anyString(), any());

        assertThatThrownBy(() -> sut.banUser(new BanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID, null)))
                .isInstanceOf(ChatProviderUnavailableException.class);

        // The ban was committed by banIfAbsent (its own transaction) and is never rolled back or deleted.
        verify(chatBanPort).banIfAbsent(any());
        verify(chatBanPort, org.mockito.Mockito.never()).delete(any(), any());
    }

    // ── unban ────────────────────────────────────────────────────────────────

    @Test
    void unban_owner_deletesBanOfTheSeller() {
        var live = given(activeLiveWithRoom());

        sut.unbanUser(new UnbanChatUserCommand(live.getId(), SELLER_ID, BUYER_ID));

        verify(chatBanPort).delete(SELLER_ID, BUYER_ID);
    }

    @Test
    void unban_nonOwner_throwsNotOwned() {
        var live = given(activeLiveWithRoom());

        assertThatThrownBy(() -> sut.unbanUser(new UnbanChatUserCommand(live.getId(), OTHER_SELLER_ID, BUYER_ID)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);

        verifyNoInteractions(chatBanPort);
    }

    // ── list bans ────────────────────────────────────────────────────────────

    private static ChatBan banAt(UUID userId, String createdAt) {
        var ban = ChatBan.create(SELLER_ID, userId, null, SELLER_ID);
        ReflectionTestUtils.setField(ban, "createdAt", OffsetDateTime.parse(createdAt));
        return ban;
    }

    @Test
    void listBans_owner_returnsSellerBansInPortOrder_withNamesResolvedInOneBatch() {
        var live = given(activeLiveWithRoom());
        var newer = UUID.randomUUID();
        var older = UUID.randomUUID();
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS)).thenReturn(List.of(
                banAt(newer, "2026-11-24T09:04:05-06:00"),
                banAt(older, "2026-11-20T10:00:00Z")));
        when(loadUserNamesPort.loadNames(any())).thenReturn(Map.of(newer, "  Bob Buyer  ", older, "Ann"));

        var result = sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID));

        assertThat(result).containsExactly(
                new BannedChatUser(newer, "Bob Buyer", Instant.parse("2026-11-24T15:04:05Z")),
                new BannedChatUser(older, "Ann", Instant.parse("2026-11-20T10:00:00Z")));
        verify(loadUserNamesPort, times(1)).loadNames(Set.of(newer, older));
    }

    @Test
    void listBans_noBans_returnsEmptyList_withoutNameLookup() {
        var live = given(activeLiveWithRoom());
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS)).thenReturn(List.of());

        assertThat(sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID))).isEmpty();
        verifyNoInteractions(loadUserNamesPort);
    }

    @Test
    void listBans_unknownName_fallsBackToUser() {
        var live = given(activeLiveWithRoom());
        var userId = UUID.randomUUID();
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS))
                .thenReturn(List.of(banAt(userId, "2026-11-20T10:00:00Z")));
        when(loadUserNamesPort.loadNames(any())).thenReturn(Map.of());

        var result = sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID));

        assertThat(result.getFirst().displayName()).isEqualTo("User");
    }

    @Test
    void listBans_nameLookupFails_stillListsBansWithFallbackName() {
        var live = given(activeLiveWithRoom());
        var userId = UUID.randomUUID();
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS))
                .thenReturn(List.of(banAt(userId, "2026-11-20T10:00:00Z")));
        when(loadUserNamesPort.loadNames(any())).thenThrow(new IllegalStateException("db down"));

        var result = sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID));

        assertThat(result).extracting(BannedChatUser::displayName).containsExactly("User");
    }

    @Test
    void listBans_endedLive_isAllowed_becauseBansArePerSeller() {
        var live = activeLiveWithRoom();
        live.end();
        given(live);
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS)).thenReturn(List.of());

        assertThat(sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID))).isEmpty();
    }

    @Test
    void listBans_scheduledLiveWithoutRoom_isAllowed() {
        var live = given(Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60));
        when(chatBanPort.findBySeller(SELLER_ID, ModerateLiveChatService.MAX_LISTED_BANS)).thenReturn(List.of());

        assertThat(sut.listBans(new ListChatBansQuery(live.getId(), SELLER_ID))).isEmpty();
    }

    @Test
    void listBans_nonOwner_throwsNotOwned_andReadsNoBans() {
        var live = given(activeLiveWithRoom());

        assertThatThrownBy(() -> sut.listBans(new ListChatBansQuery(live.getId(), OTHER_SELLER_ID)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);

        verifyNoInteractions(chatBanPort, loadUserNamesPort);
    }

    @Test
    void listBans_unknownLive_throwsNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.listBans(new ListChatBansQuery(liveId, SELLER_ID)))
                .isInstanceOf(LiveNotFoundException.class);

        verifyNoInteractions(chatBanPort, loadUserNamesPort);
    }

    // ── delete message ───────────────────────────────────────────────────────

    @Test
    void deleteMessage_owner_deletesInIvs_thenMarksMessageOfThisLive() {
        var live = given(activeLiveWithRoom());
        when(saveChatMessagePort.markDeleted(eq(live.getId()), eq(MSG_ID), any())).thenReturn(1);

        sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID));

        InOrder order = inOrder(chatModerationPort, saveChatMessagePort);
        order.verify(chatModerationPort).deleteMessage(ROOM_ARN, MSG_ID);
        order.verify(saveChatMessagePort).markDeleted(eq(live.getId()), eq(MSG_ID), any());
    }

    @Test
    void deleteMessage_noPersistedRow_stillSucceeds() {
        var live = given(activeLiveWithRoom());
        when(saveChatMessagePort.markDeleted(eq(live.getId()), eq(MSG_ID), any())).thenReturn(0);

        sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID));

        verify(chatModerationPort).deleteMessage(ROOM_ARN, MSG_ID);
    }

    @Test
    void deleteMessage_ivsFails_noDbChange() {
        var live = given(activeLiveWithRoom());
        doThrow(new ChatProviderUnavailableException("down", new RuntimeException()))
                .when(chatModerationPort).deleteMessage(anyString(), anyString());

        assertThatThrownBy(() -> sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID)))
                .isInstanceOf(ChatProviderUnavailableException.class);

        verifyNoInteractions(saveChatMessagePort);
    }

    @Test
    void deleteMessage_roomDeletedConcurrently_throwsRoomNotAvailable_noDbChange() {
        var live = given(activeLiveWithRoom());
        doThrow(new ChatRoomNotFoundException(new RuntimeException()))
                .when(chatModerationPort).deleteMessage(anyString(), anyString());

        assertThatThrownBy(() -> sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID)))
                .isInstanceOf(LiveChatRoomNotAvailableException.class);

        verifyNoInteractions(saveChatMessagePort);
    }

    @Test
    void deleteMessage_nonOwner_throwsNotOwned() {
        var live = given(activeLiveWithRoom());

        assertThatThrownBy(() -> sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), OTHER_SELLER_ID, MSG_ID)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);

        verifyNoInteractions(chatModerationPort, saveChatMessagePort);
    }

    @Test
    void deleteMessage_liveWithoutRoom_throwsRoomNotAvailable() {
        var live = given(activeLiveWithoutRoom());

        assertThatThrownBy(() -> sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID)))
                .isInstanceOf(LiveChatRoomNotAvailableException.class);

        verifyNoInteractions(chatModerationPort, saveChatMessagePort);
    }

    @Test
    void deleteMessage_endedLive_throwsNotLive_becauseTheRoomIsGone() {
        var live = activeLiveWithRoom();
        live.end();
        given(live);

        assertThatThrownBy(() -> sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID)))
                .isInstanceOf(LiveNotLiveException.class);

        verifyNoInteractions(chatModerationPort, saveChatMessagePort);
    }

    @Test
    void deleteMessage_reconnectingLive_isAllowed() {
        var live = activeLiveWithRoom();
        live.beginReconnecting();
        given(live);

        sut.deleteMessage(new DeleteChatMessageCommand(live.getId(), SELLER_ID, MSG_ID));

        verify(chatModerationPort).deleteMessage(ROOM_ARN, MSG_ID);
    }
}
