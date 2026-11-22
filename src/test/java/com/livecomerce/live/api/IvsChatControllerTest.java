package com.livecomerce.live.api;

import com.livecomerce.auth.infrastructure.security.ProductionSecurityTestConfig;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase.GetIvsChatTokenCommand;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase.IvsChatTokenResult;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against the production security filter chain. */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = IvsChatController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(ProductionSecurityTestConfig.class)
@TestPropertySource(properties = "app.frontend-url=http://localhost:3000")
class IvsChatControllerTest {

    private static final String  PATH    = "/api/lives/{liveId}/chat/token";
    private static final UUID    LIVE_ID = UUID.randomUUID();
    private static final UUID    USER_ID = UUID.randomUUID();

    @Autowired MockMvc mvc;

    @MockitoBean GetIvsChatTokenUseCase getIvsChatTokenUseCase;

    private static RequestPostProcessor as(String role) {
        var principal = new UserPrincipal(USER_ID, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_BUYER", "ROLE_SELLER"})
    void getToken_authenticated_returns200WithTokenShape(String role) throws Exception {
        when(getIvsChatTokenUseCase.getIvsChatToken(any())).thenReturn(new IvsChatTokenResult(
                "ivs-token",
                Instant.parse("2026-11-18T11:00:00Z"),
                Instant.parse("2026-11-18T10:05:00Z"),
                "us-east-1"));

        mvc.perform(get(PATH, LIVE_ID).with(as(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("ivs-token"))
                .andExpect(jsonPath("$.sessionExpirationTime").value("2026-11-18T11:00:00Z"))
                .andExpect(jsonPath("$.tokenExpirationTime").value("2026-11-18T10:05:00Z"))
                .andExpect(jsonPath("$.region").value("us-east-1"));

        verify(getIvsChatTokenUseCase).getIvsChatToken(new GetIvsChatTokenCommand(LIVE_ID, USER_ID));
    }

    @Test
    void getToken_unauthenticated_returns401() throws Exception {
        mvc.perform(get(PATH, LIVE_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getIvsChatTokenUseCase);
    }

    @Test
    void getToken_liveNotFound_returns404() throws Exception {
        when(getIvsChatTokenUseCase.getIvsChatToken(any())).thenThrow(new LiveNotFoundException(LIVE_ID));

        mvc.perform(get(PATH, LIVE_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getToken_liveNotActive_returns403() throws Exception {
        when(getIvsChatTokenUseCase.getIvsChatToken(any())).thenThrow(new LiveNotLiveException(LIVE_ID));

        mvc.perform(get(PATH, LIVE_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-not-active"));
    }

    @Test
    void getToken_liveWithoutChatRoom_returns409() throws Exception {
        when(getIvsChatTokenUseCase.getIvsChatToken(any()))
                .thenThrow(new LiveChatRoomNotAvailableException(LIVE_ID));

        mvc.perform(get(PATH, LIVE_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-chat-room-not-available"));
    }

    @Test
    void getToken_chatProviderDown_returns503() throws Exception {
        when(getIvsChatTokenUseCase.getIvsChatToken(any()))
                .thenThrow(new ChatProviderUnavailableException("down", new RuntimeException("aws")));

        mvc.perform(get(PATH, LIVE_ID).with(as("ROLE_BUYER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/chat-provider-unavailable"));
    }
}
