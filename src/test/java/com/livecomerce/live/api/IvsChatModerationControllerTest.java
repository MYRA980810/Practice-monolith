package com.livecomerce.live.api;

import com.livecomerce.auth.infrastructure.security.ProductionSecurityTestConfig;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BanChatUserCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BannedChatUser;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.DeleteChatMessageCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.ListChatBansQuery;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.UnbanChatUserCommand;
import com.livecomerce.live.domain.ChatProviderMisconfiguredException;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.ChatRequestRejectedException;
import com.livecomerce.live.domain.ChatSelfBanException;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against the production security filter chain. */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = IvsChatModerationController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(ProductionSecurityTestConfig.class)
@TestPropertySource(properties = "app.frontend-url=http://localhost:3000")
class IvsChatModerationControllerTest {

    private static final String BANS_PATH    = "/api/lives/{liveId}/chat/bans";
    private static final String BAN_PATH     = "/api/lives/{liveId}/chat/bans/{userId}";
    private static final String MESSAGE_PATH = "/api/lives/{liveId}/chat/messages/{messageId}";
    private static final UUID   LIVE_ID      = UUID.randomUUID();
    private static final UUID   SELLER_ID    = UUID.randomUUID();
    private static final UUID   TARGET_ID    = UUID.randomUUID();

    @Autowired MockMvc mvc;

    @MockitoBean ModerateLiveChatUseCase moderateLiveChatUseCase;

    private static RequestPostProcessor as(String role) {
        var principal = new UserPrincipal(SELLER_ID, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static String banBody(Object userId, String reason) {
        var reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        var userJson   = userId == null ? "null" : "\"" + userId + "\"";
        return "{\"userId\":" + userJson + ",\"reason\":" + reasonJson + "}";
    }

    // ── POST /bans ───────────────────────────────────────────────────────────

    @Test
    void ban_seller_returns204() throws Exception {
        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, "spam")))
                .andExpect(status().isNoContent());

        verify(moderateLiveChatUseCase).banUser(new BanChatUserCommand(LIVE_ID, SELLER_ID, TARGET_ID, "spam"));
    }

    @Test
    void ban_buyer_returns403() throws Exception {
        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_BUYER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, null)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void ban_unauthenticated_returns401() throws Exception {
        mvc.perform(post(BANS_PATH, LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, null)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void ban_nonOwnerSeller_returns403() throws Exception {
        doThrow(new LiveNotOwnedBySellerException(LIVE_ID, SELLER_ID))
                .when(moderateLiveChatUseCase).banUser(any());

        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-not-owned"));
    }

    @Test
    void ban_missingUserId_returns400() throws Exception {
        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(null, "spam")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void ban_reasonTooLong_returns400() throws Exception {
        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, "x".repeat(257))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void ban_self_returns422() throws Exception {
        doThrow(new ChatSelfBanException()).when(moderateLiveChatUseCase).banUser(any());

        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(SELLER_ID, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/chat-self-ban"))
                .andExpect(jsonPath("$.code").value("CHAT_SELF_BAN"));
    }

    @Test
    void ban_disconnectFails_returns503() throws Exception {
        doThrow(new ChatProviderUnavailableException("Unable to disconnect chat user", new RuntimeException()))
                .when(moderateLiveChatUseCase).banUser(any());

        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, null)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/chat-provider-unavailable"));
    }

    // ── GET /bans ────────────────────────────────────────────────────────────

    @Test
    void listBans_owner_returns200_withBansInUseCaseOrder_andUtcTimestamps() throws Exception {
        var newer = UUID.randomUUID();
        var older = UUID.randomUUID();
        when(moderateLiveChatUseCase.listBans(new ListChatBansQuery(LIVE_ID, SELLER_ID))).thenReturn(List.of(
                new BannedChatUser(newer, "Bob Buyer", Instant.parse("2026-11-24T15:04:05Z")),
                new BannedChatUser(older, "User", Instant.parse("2026-11-20T10:00:00Z"))));

        mvc.perform(get(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(newer.toString()))
                .andExpect(jsonPath("$[0].displayName").value("Bob Buyer"))
                .andExpect(jsonPath("$[0].bannedAt").value("2026-11-24T15:04:05Z"))
                .andExpect(jsonPath("$[1].userId").value(older.toString()))
                .andExpect(jsonPath("$[1].displayName").value("User"))
                .andExpect(jsonPath("$[1].bannedAt").value("2026-11-20T10:00:00Z"))
                .andExpect(jsonPath("$[0].length()").value(3));
    }

    @Test
    void listBans_noBans_returnsEmptyArray() throws Exception {
        when(moderateLiveChatUseCase.listBans(any())).thenReturn(List.of());

        mvc.perform(get(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listBans_nonOwnerSeller_returns403() throws Exception {
        when(moderateLiveChatUseCase.listBans(any())).thenThrow(new LiveNotOwnedBySellerException(LIVE_ID, SELLER_ID));

        mvc.perform(get(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-not-owned"));
    }

    @Test
    void listBans_unknownLive_returns404WithCode() throws Exception {
        when(moderateLiveChatUseCase.listBans(any())).thenThrow(new LiveNotFoundException(LIVE_ID));

        mvc.perform(get(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIVE_NOT_FOUND"));
    }

    @Test
    void listBans_buyer_returns403() throws Exception {
        mvc.perform(get(BANS_PATH, LIVE_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void listBans_unauthenticated_returns401() throws Exception {
        mvc.perform(get(BANS_PATH, LIVE_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    // ── DELETE /bans/{userId} ────────────────────────────────────────────────

    @Test
    void unban_seller_returns204() throws Exception {
        mvc.perform(delete(BAN_PATH, LIVE_ID, TARGET_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isNoContent());

        verify(moderateLiveChatUseCase).unbanUser(new UnbanChatUserCommand(LIVE_ID, SELLER_ID, TARGET_ID));
    }

    @Test
    void unban_buyer_returns403() throws Exception {
        mvc.perform(delete(BAN_PATH, LIVE_ID, TARGET_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void unban_unauthenticated_returns401() throws Exception {
        mvc.perform(delete(BAN_PATH, LIVE_ID, TARGET_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unban_nonOwnerSeller_returns403() throws Exception {
        doThrow(new LiveNotOwnedBySellerException(LIVE_ID, SELLER_ID))
                .when(moderateLiveChatUseCase).unbanUser(any());

        mvc.perform(delete(BAN_PATH, LIVE_ID, TARGET_ID).with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /messages/{messageId} ─────────────────────────────────────────

    @Test
    void deleteMessage_seller_returns204() throws Exception {
        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_SELLER")))
                .andExpect(status().isNoContent());

        verify(moderateLiveChatUseCase).deleteMessage(new DeleteChatMessageCommand(LIVE_ID, SELLER_ID, "ivs-msg-1"));
    }

    @Test
    void deleteMessage_buyer_returns403() throws Exception {
        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_BUYER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void deleteMessage_unauthenticated_returns401() throws Exception {
        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMessage_nonOwnerSeller_returns403() throws Exception {
        doThrow(new LiveNotOwnedBySellerException(LIVE_ID, SELLER_ID))
                .when(moderateLiveChatUseCase).deleteMessage(any());

        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMessage_liveWithoutRoom_returns409() throws Exception {
        doThrow(new LiveChatRoomNotAvailableException(LIVE_ID)).when(moderateLiveChatUseCase).deleteMessage(any());

        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_SELLER")))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteMessage_blankMessageId_returns400_withoutCallingTheUseCase() throws Exception {
        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, " ").with(as("ROLE_SELLER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void deleteMessage_tooLongMessageId_returns400_withoutCallingTheUseCase() throws Exception {
        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "a".repeat(129)).with(as("ROLE_SELLER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moderateLiveChatUseCase);
    }

    @Test
    void deleteMessage_providerRejectsRequest_returns400WithGenericBody() throws Exception {
        doThrow(new ChatRequestRejectedException(new RuntimeException("arn:aws:ivschat:secret")))
                .when(moderateLiveChatUseCase).deleteMessage(any());

        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_SELLER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/chat-request-rejected"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("arn:"))));
    }

    @Test
    void ban_providerMisconfigured_returns500WithGenericBody() throws Exception {
        doThrow(new ChatProviderMisconfiguredException("DisconnectUser", new RuntimeException("arn:aws:iam:secret")))
                .when(moderateLiveChatUseCase).banUser(any());

        mvc.perform(post(BANS_PATH, LIVE_ID).with(as("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(banBody(TARGET_ID, null)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/chat-provider-misconfigured"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("arn:"))));
    }

    @Test
    void deleteMessage_providerDown_returns503() throws Exception {
        doThrow(new ChatProviderUnavailableException("Unable to delete chat message", new RuntimeException()))
                .when(moderateLiveChatUseCase).deleteMessage(any());

        mvc.perform(delete(MESSAGE_PATH, LIVE_ID, "ivs-msg-1").with(as("ROLE_SELLER")))
                .andExpect(status().isServiceUnavailable());
    }
}
