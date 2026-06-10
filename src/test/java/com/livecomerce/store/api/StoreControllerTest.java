package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.SlugAlreadyTakenException;
import com.livecomerce.store.application.StoreAlreadyExistsException;
import com.livecomerce.store.application.StoreNotFoundException;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.ReactivateStoreUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
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
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(StoreControllerTest.SecurityResolverConfig.class)
class StoreControllerTest {

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

    private static Store buildStore() {
        return Store.create(USER_ID, "Mi Tienda", "mi-tienda", "Descripción", null);
    }

    // --- POST /api/stores ---

    @Test
    void create_withValidRequest_returns201WithStore() throws Exception {
        when(createStoreUseCase.create(any())).thenReturn(buildStore());

        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mi Tienda","slug":"mi-tienda","description":"Descripción"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mi Tienda"))
                .andExpect(jsonPath("$.slug").value("mi-tienda"));
    }

    @Test
    void create_whenUserAlreadyHasStore_returns409() throws Exception {
        when(createStoreUseCase.create(any())).thenThrow(new StoreAlreadyExistsException(USER_ID));

        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mi Tienda","slug":"mi-tienda"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/store-already-exists"));
    }

    @Test
    void create_whenSlugTaken_returns409() throws Exception {
        when(createStoreUseCase.create(any())).thenThrow(new SlugAlreadyTakenException("mi-tienda"));

        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mi Tienda","slug":"mi-tienda"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/slug-already-taken"));
    }

    @Test
    void create_withInvalidSlug_returns400() throws Exception {
        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mi Tienda","slug":"MI TIENDA con espacios"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_withMissingRequiredFields_returns400() throws Exception {
        mvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // --- GET /api/stores/me ---

    @Test
    void getMyStore_whenStoreExists_returns200() throws Exception {
        when(getStoreUseCase.getByUserId(USER_ID)).thenReturn(buildStore());

        mvc.perform(get("/api/stores/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("mi-tienda"));
    }

    @Test
    void getMyStore_whenNotFound_returns404() throws Exception {
        when(getStoreUseCase.getByUserId(USER_ID)).thenThrow(new StoreNotFoundException(USER_ID.toString()));

        mvc.perform(get("/api/stores/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/store-not-found"));
    }

    // --- GET /api/stores/{slug} ---

    @Test
    void getBySlug_whenStoreExists_returns200() throws Exception {
        when(getStoreUseCase.getBySlug("mi-tienda")).thenReturn(buildStore());

        mvc.perform(get("/api/stores/mi-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mi Tienda"));
    }

    @Test
    void getBySlug_whenNotFound_returns404() throws Exception {
        when(getStoreUseCase.getBySlug("no-existe")).thenThrow(new StoreNotFoundException("no-existe"));

        mvc.perform(get("/api/stores/no-existe"))
                .andExpect(status().isNotFound());
    }
}
