package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.LoadStoreRatingPort;
import com.livecomerce.store.StoreCategoryPort;
import com.livecomerce.store.StoreCategoryPort.CategoryRef;
import com.livecomerce.store.application.InvalidStoreCategoryException;
import com.livecomerce.store.application.port.in.*;
import com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort;
import com.livecomerce.store.application.port.out.LoadStoreRankPort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Store category (manual override vs inferred) on store responses, plus PUT /api/stores/me/category.
 * Enables method security so the endpoint's @PreAuthorize is actually enforced.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(StoreCategoryControllerTest.SecurityResolverConfig.class)
class StoreCategoryControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
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
    @MockitoBean LoadStoreRatingPort loadStoreRatingPort;
    @MockitoBean LoadStoreRankPort loadStoreRankPort;
    @MockitoBean LoadStoreLiveStatusPort loadStoreLiveStatusPort;
    @MockitoBean StoreCategoryPort storeCategoryPort;
    @MockitoBean SetStoreCategoryUseCase setStoreCategoryUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CategoryRef MODA = new CategoryRef(UUID.randomUUID(), "Moda", "moda");
    private static final CategoryRef HOGAR = new CategoryRef(UUID.randomUUID(), "Hogar", "hogar");

    private void authenticateAs(String role) {
        var principal = new UserPrincipal(
                USER_ID, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Store buildStore(String slug) {
        return Store.create(USER_ID, "Tienda " + slug, slug, null, null);
    }

    // --- GET /api/stores (cards) ---

    @Test
    void listStores_resolvesManualInferredAndMissingCategoriesInBatch() throws Exception {
        var manual = buildStore("manual");
        manual.changeCategory(MODA.id());
        var inferred = buildStore("inferred");
        var none = buildStore("none");
        when(listStoresUseCase.listActive(any()))
                .thenReturn(new PageImpl<>(List.of(manual, inferred, none), PageRequest.of(0, 20), 3));
        when(storeCategoryPort.loadActiveByIds(Set.of(MODA.id()))).thenReturn(Map.of(MODA.id(), MODA));
        when(storeCategoryPort.inferTopByStore(Set.of(inferred.getId(), none.getId())))
                .thenReturn(Map.of(inferred.getId(), HOGAR));

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.id").value(MODA.id().toString()))
                .andExpect(jsonPath("$.content[0].category.name").value("Moda"))
                .andExpect(jsonPath("$.content[0].category.slug").value("moda"))
                .andExpect(jsonPath("$.content[0].category.source").value("MANUAL"))
                .andExpect(jsonPath("$.content[1].category.id").value(HOGAR.id().toString()))
                .andExpect(jsonPath("$.content[1].category.source").value("INFERRED"))
                .andExpect(jsonPath("$.content[2].category").isEmpty());
    }

    @Test
    void listStores_manualOverrideNoLongerActive_fallsBackToInference() throws Exception {
        var store = buildStore("stale");
        var inactiveId = UUID.randomUUID();
        store.changeCategory(inactiveId);
        when(listStoresUseCase.listActive(any()))
                .thenReturn(new PageImpl<>(List.of(store), PageRequest.of(0, 20), 1));
        when(storeCategoryPort.loadActiveByIds(Set.of(inactiveId))).thenReturn(Map.of());
        when(storeCategoryPort.inferTopByStore(Set.of(store.getId()))).thenReturn(Map.of(store.getId(), HOGAR));

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.id").value(HOGAR.id().toString()))
                .andExpect(jsonPath("$.content[0].category.source").value("INFERRED"));
    }

    @Test
    void listStores_allManualResolved_skipsInference() throws Exception {
        var store = buildStore("manual");
        store.changeCategory(MODA.id());
        when(listStoresUseCase.listActive(any()))
                .thenReturn(new PageImpl<>(List.of(store), PageRequest.of(0, 20), 1));
        when(storeCategoryPort.loadActiveByIds(any())).thenReturn(Map.of(MODA.id(), MODA));

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.source").value("MANUAL"));

        verify(storeCategoryPort, never()).inferTopByStore(any());
    }

    @Test
    void listStores_whenCategoryPortThrows_returns200WithNullCategory() throws Exception {
        var store = buildStore("inferred");
        when(listStoresUseCase.listActive(any()))
                .thenReturn(new PageImpl<>(List.of(store), PageRequest.of(0, 20), 1));
        when(storeCategoryPort.inferTopByStore(any())).thenThrow(new RuntimeException("catalog down"));

        mvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("inferred"))
                .andExpect(jsonPath("$.content[0].category").isEmpty());
    }

    // --- GET /api/stores/{slug} ---

    @Test
    void getBySlug_includesInferredCategory() throws Exception {
        var store = buildStore("mi-tienda");
        when(getStoreUseCase.getBySlug("mi-tienda")).thenReturn(store);
        when(storeCategoryPort.inferTopByStore(Set.of(store.getId()))).thenReturn(Map.of(store.getId(), HOGAR));

        mvc.perform(get("/api/stores/mi-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Hogar"))
                .andExpect(jsonPath("$.category.source").value("INFERRED"));
    }

    @Test
    void getBySlug_withManualOverride_showsManual() throws Exception {
        var store = buildStore("mi-tienda");
        store.changeCategory(MODA.id());
        when(getStoreUseCase.getBySlug("mi-tienda")).thenReturn(store);
        when(storeCategoryPort.loadActiveByIds(Set.of(MODA.id()))).thenReturn(Map.of(MODA.id(), MODA));

        mvc.perform(get("/api/stores/mi-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(MODA.id().toString()))
                .andExpect(jsonPath("$.category.source").value("MANUAL"));
    }

    // --- GET /api/stores/me ---

    @Test
    void getMyStore_includesManualCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        var store = buildStore("mi-tienda");
        store.changeCategory(MODA.id());
        when(getStoreUseCase.getByUserId(USER_ID)).thenReturn(store);
        when(storeCategoryPort.loadActiveByIds(Set.of(MODA.id()))).thenReturn(Map.of(MODA.id(), MODA));

        mvc.perform(get("/api/stores/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(MODA.id().toString()))
                .andExpect(jsonPath("$.category.source").value("MANUAL"));
    }

    @Test
    void getMyStore_withoutAnyCategory_returnsNullCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        when(getStoreUseCase.getByUserId(USER_ID)).thenReturn(buildStore("mi-tienda"));

        mvc.perform(get("/api/stores/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").isEmpty());
    }

    @Test
    void getMyStore_whenCategoryPortThrows_returns200WithNullCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        var store = buildStore("mi-tienda");
        store.changeCategory(MODA.id());
        when(getStoreUseCase.getByUserId(USER_ID)).thenReturn(store);
        when(storeCategoryPort.loadActiveByIds(any())).thenThrow(new RuntimeException("catalog down"));

        mvc.perform(get("/api/stores/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("mi-tienda"))
                .andExpect(jsonPath("$.category").isEmpty());
    }

    @Test
    void updateMyStore_responseKeepsResolvedCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        var store = buildStore("mi-tienda");
        when(updateStoreUseCase.update(any())).thenReturn(store);
        when(storeCategoryPort.inferTopByStore(Set.of(store.getId()))).thenReturn(Map.of(store.getId(), HOGAR));

        mvc.perform(put("/api/stores/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tienda mi-tienda"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.source").value("INFERRED"));
    }

    // --- PUT /api/stores/me/category ---

    @Test
    void setMyCategory_withCategory_returns200WithManualCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        var store = buildStore("mi-tienda");
        store.changeCategory(MODA.id());
        when(setStoreCategoryUseCase.setCategory(USER_ID, MODA.id())).thenReturn(store);
        when(storeCategoryPort.loadActiveByIds(Set.of(MODA.id()))).thenReturn(Map.of(MODA.id(), MODA));

        mvc.perform(put("/api/stores/me/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + MODA.id() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(MODA.id().toString()))
                .andExpect(jsonPath("$.category.source").value("MANUAL"));
    }

    @Test
    void setMyCategory_withNull_clearsOverrideAndReturnsInferredCategory() throws Exception {
        authenticateAs("ROLE_SELLER");
        var store = buildStore("mi-tienda");
        when(setStoreCategoryUseCase.setCategory(eq(USER_ID), isNull())).thenReturn(store);
        when(storeCategoryPort.inferTopByStore(Set.of(store.getId()))).thenReturn(Map.of(store.getId(), HOGAR));

        mvc.perform(put("/api/stores/me/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(HOGAR.id().toString()))
                .andExpect(jsonPath("$.category.source").value("INFERRED"));

        verify(setStoreCategoryUseCase).setCategory(eq(USER_ID), isNull());
    }

    @Test
    void setMyCategory_withInvalidCategory_returns422ProblemDetail() throws Exception {
        authenticateAs("ROLE_SELLER");
        var categoryId = UUID.randomUUID();
        when(setStoreCategoryUseCase.setCategory(USER_ID, categoryId))
                .thenThrow(new InvalidStoreCategoryException(categoryId));

        mvc.perform(put("/api/stores/me/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/invalid-store-category"));
    }

    @Test
    void setMyCategory_asNonSeller_returns403() throws Exception {
        authenticateAs("ROLE_BUYER");

        mvc.perform(put("/api/stores/me/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":null}"))
                .andExpect(status().isForbidden());

        verify(setStoreCategoryUseCase, never()).setCategory(any(), any());
    }
}
