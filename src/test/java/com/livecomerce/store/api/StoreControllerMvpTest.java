package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.StoreNotFoundException;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
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
    @MockitoBean ListStoresUseCase listStoresUseCase;

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
        var store = Store.create(UUID.randomUUID(), "Tienda A", "tienda-a", "Descripción");
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
}
