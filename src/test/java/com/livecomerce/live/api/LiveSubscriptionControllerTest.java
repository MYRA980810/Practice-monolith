package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.CheckLiveSubscriptionUseCase;
import com.livecomerce.live.application.port.in.SubscribeToLiveUseCase;
import com.livecomerce.live.application.port.in.UnsubscribeFromLiveUseCase;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotScheduledException;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveSubscriptionController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(LiveSubscriptionControllerTest.SecurityResolverConfig.class)
class LiveSubscriptionControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean SubscribeToLiveUseCase      subscribeToLiveUseCase;
    @MockitoBean UnsubscribeFromLiveUseCase  unsubscribeFromLiveUseCase;
    @MockitoBean CheckLiveSubscriptionUseCase checkLiveSubscriptionUseCase;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID LIVE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUpBuyerPrincipal() {
        setPrincipal(BUYER_ID, "ROLE_BUYER");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- POST /api/lives/{liveId}/subscribe ---

    @Test
    void subscribe_asBuyer_returns200() throws Exception {
        mvc.perform(post("/api/lives/{liveId}/subscribe", LIVE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void subscribe_toNonScheduledLive_returns400() throws Exception {
        doThrow(new LiveNotScheduledException(LIVE_ID))
                .when(subscribeToLiveUseCase).subscribe(any());

        mvc.perform(post("/api/lives/{liveId}/subscribe", LIVE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-not-scheduled"));
    }

    @Test
    void subscribe_asSeller_returns403() throws Exception {
        setPrincipal(UUID.randomUUID(), "ROLE_SELLER");

        mvc.perform(post("/api/lives/{liveId}/subscribe", LIVE_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-subscription-buyers-only"));
    }

    // --- DELETE /api/lives/{liveId}/subscribe ---

    @Test
    void unsubscribe_asBuyer_returns200Idempotent() throws Exception {
        mvc.perform(delete("/api/lives/{liveId}/subscribe", LIVE_ID))
                .andExpect(status().isOk());
    }

    // --- GET /api/lives/{liveId}/subscribed ---

    @Test
    void isSubscribed_whenSubscribed_returnsTrue() throws Exception {
        when(checkLiveSubscriptionUseCase.isSubscribed(any())).thenReturn(true);

        mvc.perform(get("/api/lives/{liveId}/subscribed", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true));
    }

    @Test
    void isSubscribed_whenNotSubscribed_returnsFalse() throws Exception {
        when(checkLiveSubscriptionUseCase.isSubscribed(any())).thenReturn(false);

        mvc.perform(get("/api/lives/{liveId}/subscribed", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));
    }
}
