package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.store.LoadStoreRatingPort;
import com.livecomerce.store.StoreCategoryPort;
import com.livecomerce.store.api.StoreController;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CloseStoreTemporarilyUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.FollowStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.ReactivateStoreUseCase;
import com.livecomerce.store.application.port.in.ReopenStoreUseCase;
import com.livecomerce.store.application.port.in.SetStoreCategoryUseCase;
import com.livecomerce.store.application.port.in.UnfollowStoreUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort;
import com.livecomerce.store.application.port.out.LoadStoreRankPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies GET /api/stores/{id}/followers/count and GET /api/stores/{id}/following
 * are reachable without a JWT — the store card for an anonymous visitor must not
 * get a 401 from the global filter chain (SecurityConfig's permitAll whitelist).
 * Must be in the auth.infrastructure.security package to reference package-private
 * bean types (same pattern as AgoraSignalingPermitAllTest).
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "jwt.secret=livecomerce-secret-key-change-this-in-production-min-32chars",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=2592000000"
})
class StoreFollowerPermitAllTest {

    @Autowired
    MockMvc mvc;

    // Package-private beans in auth.infrastructure.security — accessible from this package
    @MockitoBean JwtService                                 jwtService;
    @MockitoBean UserDetailsAdapter                         userDetailsAdapter;
    @MockitoBean GoogleOAuth2SuccessHandler                 googleOAuth2SuccessHandler;
    @MockitoBean CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    @MockitoBean RestAuthenticationEntryPoint               restAuthenticationEntryPoint;

    // OAuth2 login requires a ClientRegistrationRepository even when autoconfiguration is excluded
    @MockitoBean ClientRegistrationRepository clientRegistrationRepository;

    // StoreController's own dependencies
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
    @MockitoBean LoadStoreRatingPort loadStoreRatingPort;
    @MockitoBean LoadStoreRankPort loadStoreRankPort;
    @MockitoBean LoadStoreLiveStatusPort loadStoreLiveStatusPort;
    @MockitoBean StoreCategoryPort storeCategoryPort;
    @MockitoBean SetStoreCategoryUseCase setStoreCategoryUseCase;

    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void followerCount_withNoJwt_isNotRejectedWith401() throws Exception {
        when(getStoreFollowersUseCase.getFollowerCount(STORE_ID)).thenReturn(3L);

        mvc.perform(get("/api/stores/{storeId}/followers/count", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(3));
    }

    @Test
    void isFollowing_withNoJwt_isNotRejectedWith401_andReturnsFalse() throws Exception {
        mvc.perform(get("/api/stores/{storeId}/following", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));

        verify(getStoreFollowersUseCase, never()).isFollowing(any(), any());
    }
}
