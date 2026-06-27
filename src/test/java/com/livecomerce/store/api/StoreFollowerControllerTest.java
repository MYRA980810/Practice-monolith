package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.StoreNotFoundException;
import com.livecomerce.store.application.port.in.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(StoreFollowerControllerTest.SecurityResolverConfig.class)
class StoreFollowerControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean CreateStoreUseCase createStoreUseCase;
    @MockitoBean GetStoreUseCase getStoreUseCase;
    @MockitoBean UpdateStoreUseCase updateStoreUseCase;
    @MockitoBean ChangePlanUseCase changePlanUseCase;
    @MockitoBean DeactivateStoreUseCase deactivateStoreUseCase;
    @MockitoBean ReactivateStoreUseCase reactivateStoreUseCase;
    @MockitoBean CloseStoreTemporarilyUseCase closeStoreTemporarilyUseCase;
    @MockitoBean ReopenStoreUseCase reopenStoreUseCase;
    @MockitoBean ListStoresUseCase listStoresUseCase;
    @MockitoBean FollowStoreUseCase followStoreUseCase;
    @MockitoBean UnfollowStoreUseCase unfollowStoreUseCase;
    @MockitoBean GetStoreFollowersUseCase getStoreFollowersUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                USER_ID, "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- POST /api/stores/{storeId}/follow ---

    @Test
    void followStore_returns204() throws Exception {
        doNothing().when(followStoreUseCase).follow(STORE_ID, USER_ID);

        mvc.perform(post("/api/stores/{storeId}/follow", STORE_ID))
                .andExpect(status().isNoContent());

        verify(followStoreUseCase).follow(STORE_ID, USER_ID);
    }

    @Test
    void followStore_whenStoreNotFound_returns404() throws Exception {
        doThrow(new StoreNotFoundException(STORE_ID.toString()))
                .when(followStoreUseCase).follow(STORE_ID, USER_ID);

        mvc.perform(post("/api/stores/{storeId}/follow", STORE_ID))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/stores/{storeId}/follow ---

    @Test
    void unfollowStore_returns204() throws Exception {
        doNothing().when(unfollowStoreUseCase).unfollow(STORE_ID, USER_ID);

        mvc.perform(delete("/api/stores/{storeId}/follow", STORE_ID))
                .andExpect(status().isNoContent());

        verify(unfollowStoreUseCase).unfollow(STORE_ID, USER_ID);
    }

    // --- GET /api/stores/{storeId}/followers/count ---

    @Test
    void getFollowerCount_returns200WithCount() throws Exception {
        when(getStoreFollowersUseCase.getFollowerCount(STORE_ID)).thenReturn(42L);

        mvc.perform(get("/api/stores/{storeId}/followers/count", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(42));
    }

    @Test
    void getFollowerCount_whenNoFollowers_returnsZero() throws Exception {
        when(getStoreFollowersUseCase.getFollowerCount(STORE_ID)).thenReturn(0L);

        mvc.perform(get("/api/stores/{storeId}/followers/count", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    // --- GET /api/stores/{storeId}/following ---

    @Test
    void isFollowing_whenFollowing_returnsTrueStatus() throws Exception {
        when(getStoreFollowersUseCase.isFollowing(STORE_ID, USER_ID)).thenReturn(true);

        mvc.perform(get("/api/stores/{storeId}/following", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void isFollowing_whenNotFollowing_returnsFalseStatus() throws Exception {
        when(getStoreFollowersUseCase.isFollowing(STORE_ID, USER_ID)).thenReturn(false);

        mvc.perform(get("/api/stores/{storeId}/following", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));
    }
}
