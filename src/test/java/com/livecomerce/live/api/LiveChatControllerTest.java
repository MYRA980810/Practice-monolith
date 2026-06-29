package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.GetChatHistoryUseCase;
import com.livecomerce.live.application.port.in.GetChatTokenUseCase;
import com.livecomerce.live.domain.*;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveChatController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(LiveChatControllerTest.SecurityResolverConfig.class)
class LiveChatControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean GetChatTokenUseCase   getChatTokenUseCase;
    @MockitoBean GetChatHistoryUseCase getChatHistoryUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID LIVE_ID = UUID.randomUUID();

    @BeforeEach
    void setPrincipal() {
        var principal = new UserPrincipal(USER_ID, "user@example.com", "",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getChatToken_activeLive_returns200WithToken_onRtmPath() throws Exception {
        when(getChatTokenUseCase.getChatToken(any()))
                .thenReturn(new GetChatTokenUseCase.ChatTokenResult("007token", "live-chat:" + LIVE_ID, "app-123"));

        mvc.perform(get("/api/lives/{liveId}/rtm/token", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("007token"))
                .andExpect(jsonPath("$.channelName").value("live-chat:" + LIVE_ID))
                .andExpect(jsonPath("$.appId").value("app-123"));
    }

    @Test
    void getChatToken_oldChatPath_returns404() throws Exception {
        mvc.perform(get("/api/lives/{liveId}/chat/token", LIVE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatHistory_returnsMessages() throws Exception {
        var live = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Live", null, null, 60);
        var msg  = LiveChatMessage.create(live, "msg-001", USER_ID, "alice", "Hello!", OffsetDateTime.now());
        when(getChatHistoryUseCase.getChatHistory(any())).thenReturn(List.of(msg));

        mvc.perform(get("/api/lives/{liveId}/rtm/history", LIVE_ID)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].content").value("Hello!"));
    }

}
