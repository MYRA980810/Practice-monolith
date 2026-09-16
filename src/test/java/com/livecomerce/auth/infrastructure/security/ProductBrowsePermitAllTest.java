package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.catalog.api.ProductController;
import com.livecomerce.catalog.application.port.in.AddProductImageUseCase;
import com.livecomerce.catalog.application.port.in.AddProductImagesUseCase;
import com.livecomerce.catalog.application.port.in.AddProductOptionUseCase;
import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.in.CorrectStockUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductVariantUseCase;
import com.livecomerce.catalog.application.port.in.DeactivateProductUseCase;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import com.livecomerce.catalog.application.port.in.PauseProductUseCase;
import com.livecomerce.catalog.application.port.in.RemoveProductImageUseCase;
import com.livecomerce.catalog.application.port.in.ResumeProductUseCase;
import com.livecomerce.catalog.application.port.in.UpdateOptionValueSwatchUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductUseCase;
import com.livecomerce.catalog.application.query.ProductView;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies GET /api/products/browse is reachable without a JWT — global catalog
 * browsing by category must not get a 401 from the SecurityConfig filter chain.
 * Must live in the auth.infrastructure.security package to reference the
 * package-private SecurityConfig and its collaborator beans (same pattern as
 * StoreFollowerPermitAllTest / AgoraSignalingPermitAllTest).
 *
 * This is the regression guard for the one real risk in the browse-by-category
 * feature: forgetting to add "/api/products/browse" to the permitAll whitelist.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = ProductController.class,
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
class ProductBrowsePermitAllTest {

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

    // ProductController's own dependencies
    @MockitoBean CreateProductUseCase createProductUseCase;
    @MockitoBean UpdateProductUseCase updateProductUseCase;
    @MockitoBean GetProductUseCase getProductUseCase;
    @MockitoBean ListCategoriesUseCase listCategoriesUseCase;
    @MockitoBean AddStockUseCase addStockUseCase;
    @MockitoBean CorrectStockUseCase correctStockUseCase;
    @MockitoBean AddProductImageUseCase addProductImageUseCase;
    @MockitoBean UpdateProductImageUseCase updateProductImageUseCase;
    @MockitoBean RemoveProductImageUseCase removeProductImageUseCase;
    @MockitoBean DeactivateProductUseCase deactivateProductUseCase;
    @MockitoBean PauseProductUseCase pauseProductUseCase;
    @MockitoBean ResumeProductUseCase resumeProductUseCase;
    @MockitoBean GetStoreUseCase getStoreUseCase;
    @MockitoBean AddProductOptionUseCase addProductOptionUseCase;
    @MockitoBean UpdateOptionValueSwatchUseCase updateOptionValueSwatchUseCase;
    @MockitoBean CreateProductVariantUseCase createProductVariantUseCase;
    @MockitoBean AddProductImagesUseCase addProductImagesUseCase;

    private static ProductView buildProductView() {
        var stock = new ProductView.StockInfo(0, 0, 0);
        return new ProductView(
                UUID.randomUUID(), UUID.randomUUID(), "Remera Básica", "Descripción",
                new BigDecimal("150.00"), null, null, "MXN", "SKU-001",
                true, false, null, null,
                stock, "Sin stock", 0L, List.of(), List.of(), List.of(),
                0.0, 0L, false, false,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void browse_withNoJwt_isNotRejectedWith401() throws Exception {
        var page = new PageImpl<>(List.of(buildProductView()), PageRequest.of(0, 20), 1);
        when(getProductUseCase.browse(any(), any())).thenReturn(page);

        mvc.perform(get("/api/products/browse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void browse_withCategoryIdAndNoJwt_isNotRejectedWith401() throws Exception {
        var page = new PageImpl<>(List.of(buildProductView()), PageRequest.of(0, 20), 1);
        when(getProductUseCase.browse(any(), any())).thenReturn(page);

        mvc.perform(get("/api/products/browse").param("categoryId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
