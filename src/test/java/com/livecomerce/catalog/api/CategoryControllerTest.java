package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.CategorySlugAlreadyTakenException;
import com.livecomerce.catalog.application.port.in.CreateCategoryUseCase;
import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import com.livecomerce.catalog.domain.Category;
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
        controllers = CategoryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(CategoryControllerTest.SecurityResolverConfig.class)
class CategoryControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean ListCategoriesUseCase listCategoriesUseCase;
    @MockitoBean CreateCategoryUseCase createCategoryUseCase;

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

    // --- GET /api/categories ---

    @Test
    void listCategories_returnsOk_withCategories() throws Exception {
        var c1 = Category.create("Electrónica", "electronica", null);
        var c2 = Category.create("Moda Femenina", "moda-femenina", null);
        when(listCategoriesUseCase.listActive()).thenReturn(List.of(c1, c2));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Electrónica"))
                .andExpect(jsonPath("$[0].slug").value("electronica"));
    }

    // --- POST /api/categories ---

    @Test
    void createCategory_asSeller_returns201() throws Exception {
        var category = Category.create("Electrónica", "electronica", USER_ID);
        when(createCategoryUseCase.create(any())).thenReturn(category);

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Electrónica", "slug": "electronica"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electrónica"))
                .andExpect(jsonPath("$.slug").value("electronica"));
    }

    @Test
    void createCategory_duplicateSlug_returns409() throws Exception {
        when(createCategoryUseCase.create(any())).thenThrow(new CategorySlugAlreadyTakenException("electronica"));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Electrónica", "slug": "electronica"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/category-slug-already-taken"));
    }

    @Test
    void createCategory_withInvalidSlug_returns400() throws Exception {
        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Electrónica", "slug": "Electrónica con Espacios"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
