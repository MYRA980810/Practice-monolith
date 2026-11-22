package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase.GetIvsChatTokenCommand;
import com.livecomerce.live.application.port.out.IvsChatTokenPort;
import com.livecomerce.live.application.port.out.IvsChatTokenPort.ChatToken;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIvsChatTokenServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock IvsChatTokenPort       ivsChatTokenPort;
    @Mock LoadSellerNamesPort loadUserNamesPort;

    private static final UUID    SELLER_ID      = UUID.randomUUID();
    private static final UUID    BUYER_ID       = UUID.randomUUID();
    private static final String  ROOM_ARN       = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-11-18T11:00:00Z");
    private static final Instant TOKEN_EXPIRY   = Instant.parse("2026-11-18T10:05:00Z");

    private GetIvsChatTokenService sut;

    @BeforeEach
    void setUp() {
        sut = new GetIvsChatTokenService(loadLivePort, ivsChatTokenPort, loadUserNamesPort, 60);
        lenient().when(ivsChatTokenPort.createToken(anyString(), anyString(), any(), anyInt()))
                .thenReturn(new ChatToken("ivs-token", SESSION_EXPIRY, TOKEN_EXPIRY, "us-east-1"));
    }

    private static Live liveLiveWithRoom() {
        var live = Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60);
        live.start();
        live.setChatRoom(ROOM_ARN);
        return live;
    }

    private void givenLive(Live live) {
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
    }

    private void givenName(UUID userId, String name) {
        when(loadUserNamesPort.loadNames(any())).thenReturn(name == null ? Map.of() : Map.of(userId, name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> capturedAttributes() {
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ivsChatTokenPort).createToken(eq(ROOM_ARN), anyString(), captor.capture(), anyInt());
        return captor.getValue();
    }

    @Test
    void owner_getsSellerRole_andTokenForTheLiveRoom() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(SELLER_ID, "Ana Store");

        var result = sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), SELLER_ID));

        assertThat(result.token()).isEqualTo("ivs-token");
        assertThat(result.sessionExpirationTime()).isEqualTo(SESSION_EXPIRY);
        assertThat(result.tokenExpirationTime()).isEqualTo(TOKEN_EXPIRY);
        assertThat(result.region()).isEqualTo("us-east-1");
        verify(ivsChatTokenPort).createToken(eq(ROOM_ARN), eq(SELLER_ID.toString()), any(), eq(60));
        assertThat(capturedAttributes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "liveId",      live.getId().toString(),
                "displayName", "Ana Store",
                "role",        "SELLER"));
    }

    @Test
    void nonOwner_getsBuyerRole() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, "Bob Buyer");

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        verify(ivsChatTokenPort).createToken(eq(ROOM_ARN), eq(BUYER_ID.toString()), any(), anyInt());
        assertThat(capturedAttributes())
                .containsEntry("role", "BUYER")
                .containsEntry("displayName", "Bob Buyer");
    }

    @Test
    void reconnectingLive_isAllowed() {
        var live = liveLiveWithRoom();
        live.beginReconnecting();
        givenLive(live);
        givenName(BUYER_ID, "Bob");

        var result = sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(result.token()).isEqualTo("ivs-token");
    }

    @Test
    void liveNotFound_throws() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getIvsChatToken(new GetIvsChatTokenCommand(liveId, BUYER_ID)))
                .isInstanceOf(LiveNotFoundException.class);
        verifyNoInteractions(ivsChatTokenPort);
    }

    @Test
    void scheduledLive_throwsNotLive() {
        var live = Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60);
        givenLive(live);

        assertThatThrownBy(() -> sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID)))
                .isInstanceOf(LiveNotLiveException.class);
        verifyNoInteractions(ivsChatTokenPort);
    }

    @Test
    void endedLive_throwsNotLive() {
        var live = liveLiveWithRoom();
        live.end();
        givenLive(live);

        assertThatThrownBy(() -> sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID)))
                .isInstanceOf(LiveNotLiveException.class);
        verifyNoInteractions(ivsChatTokenPort);
    }

    @Test
    void liveWithoutChatRoom_throwsChatRoomNotAvailable() {
        var live = Live.create(SELLER_ID, UUID.randomUUID(), LiveContext.STORE, "Live", "thumb", null, 60);
        live.start();
        givenLive(live);

        assertThatThrownBy(() -> sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID)))
                .isInstanceOf(LiveChatRoomNotAvailableException.class);
        verifyNoInteractions(ivsChatTokenPort);
    }

    @Test
    void longDisplayName_isTruncatedTo64Characters() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, "x".repeat(200));

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(capturedAttributes().get("displayName")).isEqualTo("x".repeat(64));
    }

    @Test
    void truncation_doesNotSplitSurrogatePairs() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, "😀".repeat(100)); // 100 emoji = 200 UTF-16 chars

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(capturedAttributes().get("displayName")).isEqualTo("😀".repeat(64));
    }

    @Test
    void blankDisplayName_fallsBackToSafeValue() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, "   ");

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(capturedAttributes().get("displayName")).isEqualTo(GetIvsChatTokenService.FALLBACK_DISPLAY_NAME);
    }

    @Test
    void unknownUser_fallsBackToSafeValue() {
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, null);

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(capturedAttributes().get("displayName")).isEqualTo(GetIvsChatTokenService.FALLBACK_DISPLAY_NAME);
    }

    @Test
    void nameLookupFailure_fallsBackToSafeValue_andStillIssuesToken() {
        var live = liveLiveWithRoom();
        givenLive(live);
        when(loadUserNamesPort.loadNames(any())).thenThrow(new IllegalStateException("db down"));

        var result = sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        assertThat(result.token()).isEqualTo("ivs-token");
        assertThat(capturedAttributes().get("displayName")).isEqualTo(GetIvsChatTokenService.FALLBACK_DISPLAY_NAME);
    }

    @Test
    void attributeKeysAndRoleValues_matchTheWireContract() {
        assertThat(GetIvsChatTokenService.ATTR_LIVE_ID).isEqualTo("liveId");
        assertThat(GetIvsChatTokenService.ATTR_DISPLAY_NAME).isEqualTo("displayName");
        assertThat(GetIvsChatTokenService.ATTR_ROLE).isEqualTo("role");
        assertThat(GetIvsChatTokenService.ChatRole.SELLER.name()).isEqualTo("SELLER");
        assertThat(GetIvsChatTokenService.ChatRole.BUYER.name()).isEqualTo("BUYER");
    }

    @Test
    void configuredSessionDuration_isPassedThrough() {
        sut = new GetIvsChatTokenService(loadLivePort, ivsChatTokenPort, loadUserNamesPort, 30);
        var live = liveLiveWithRoom();
        givenLive(live);
        givenName(BUYER_ID, "Bob");

        sut.getIvsChatToken(new GetIvsChatTokenCommand(live.getId(), BUYER_ID));

        verify(ivsChatTokenPort).createToken(anyString(), anyString(), any(), eq(30));
    }
}
