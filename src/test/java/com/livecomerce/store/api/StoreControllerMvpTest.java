package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.StoreCannotBeReactivatedException;
import com.livecomerce.store.application.StoreNotFoundException;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CloseStoreTemporarilyUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.ReactivateStoreUseCase;
import com.livecomerce.store.application.port.in.ReopenStoreUseCase;
import com.livecomerce.store.application.port.in.UnfollowStoreUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.store.application.port.in.FollowStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.domain.Store;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(StoreControllerMvpTest.SecurityResolverConfig.class)
class StoreControllerMvpTest {

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

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                USER_ID, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- DELETE /api/stores/me ---

    @Test
    void deactivate_whenStoreExists_returns204() throws Exception {
        doNothing().when(deactivateStoreUseCase).deactivate(USER_ID);

        mvc.perform(delete("/api/stores/me"))
                .andExpect(status().isNoContent());

        verify(deactivateStoreUseCase).deactivate(USER_ID);
    }

    @Test
    void deactivate_whenStoreNotFound_returns404() throws Exception {
        doThrow(new StoreNotFoundException(USER_ID.toString()))
                .when(deactivateStoreUseCase).deactivate(USER_ID);

        mvc.perform(delete("/api/stores/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/store-not-found"));
    }

    // --- GET /api/stores ---

    @Test
    void listStores_returnsPageOfActiveStores() throws Exception {
        var store = Store.create(UUID.randomUUID(), "Tienda A", "tienda-a", "Descripción", null);
        var page = new PageImpl<>(List.of(store), PageRequest.of(0, 20), 1);
        when(listStoresUseCase.listActive(any())).thenReturn(page);

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("tienda-a"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listStores_whenEmpty_returnsEmptyPage() throws Exception {
        when(listStoresUseCase.listActive(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- POST /api/stores/me/reactivate ---

    @Test
    void reactivate_whenSucceeds_returns200WithStoreResponse() throws Exception {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        doNothing().when(reactivateStoreUseCase).reactivate(USER_ID);
        when(getStoreUseCase.getByUserId(USER_ID)).thenReturn(store);

        mvc.perform(post("/api/stores/me/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.suspended").exists());

        verify(reactivateStoreUseCase).reactivate(USER_ID);
    }

    @Test
    void reactivate_whenSuspended_returns409() throws Exception {
        doThrow(new StoreCannotBeReactivatedException())
                .when(reactivateStoreUseCase).reactivate(USER_ID);

        mvc.perform(post("/api/stores/me/reactivate"))
                .andExpect(status().isConflict());
    }

    @Test
    void reactivate_whenStoreNotFound_returns404() throws Exception {
        doThrow(new StoreNotFoundException(USER_ID.toString()))
                .when(reactivateStoreUseCase).reactivate(USER_ID);

        mvc.perform(post("/api/stores/me/reactivate"))
                .andExpect(status().isNotFound());
    }
}
