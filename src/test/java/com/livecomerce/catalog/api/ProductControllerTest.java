package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.ProductNotFoundException;
import com.livecomerce.catalog.application.ProductVariantNotFoundException;
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
import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductUseCase;
import com.livecomerce.catalog.application.query.ProductView;
import com.livecomerce.catalog.application.query.VariantView;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
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

import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(ProductControllerTest.SecurityResolverConfig.class)
class ProductControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

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
    @MockitoBean CreateProductVariantUseCase createProductVariantUseCase;
    @MockitoBean AddProductImagesUseCase addProductImagesUseCase;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                STORE_ID, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static ProductView buildProductView() {
        var stock = new ProductView.StockInfo(0, 0, 0);
        return new ProductView(
                PRODUCT_ID, STORE_ID, "Remera Básica", "Descripción",
                new BigDecimal("150.00"), "MXN", "SKU-001",
                true, false, null, null,
                stock, List.of(), List.of(), List.of(),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera Básica", "Descripción", new BigDecimal("150.00"), "MXN", "SKU-001", null);
    }

    private static VariantView buildVariantView() {
        return new VariantView(
                VARIANT_ID, PRODUCT_ID, "SKU-001", null,
                new BigDecimal("150.00"), true, 0,
                List.of(),
                new VariantView.StockInfo(20, 20, 0)
        );
    }

    // --- POST /api/products ---

    @Test
    void create_withValidRequest_returns201WithProduct() throws Exception {
        when(createProductUseCase.create(any())).thenReturn(buildProduct());
        when(getProductUseCase.getById(any())).thenReturn(buildProductView());

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "name": "Remera Básica",
                                  "description": "Descripción",
                                  "basePrice": 150.00,
                                  "currency": "MXN",
                                  "sku": "SKU-001",
                                  "categoryId": "%s"
                                }
                                """.formatted(STORE_ID, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Remera Básica"))
                .andExpect(jsonPath("$.currency").value("MXN"))
                .andExpect(jsonPath("$.stock.totalQuantity").value(0))
                .andExpect(jsonPath("$.images").isArray());
    }

    @Test
    void create_withImages_returns201AndImagesArray() throws Exception {
        var imageInfo = new ProductView.ImageInfo(UUID.randomUUID(), "https://cdn.example.com/img.jpg", 0, true);
        var viewWithImages = new ProductView(
                PRODUCT_ID, STORE_ID, "Remera Básica", "Descripción",
                new BigDecimal("150.00"), "MXN", "SKU-001",
                true, false, null, null,
                new ProductView.StockInfo(0, 0, 0),
                List.of(imageInfo), List.of(), List.of(),
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now()
        );
        when(createProductUseCase.create(any())).thenReturn(buildProduct());
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(getProductUseCase.getById(any())).thenReturn(viewWithImages);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Remera Básica",
                                  "description": "Descripción",
                                  "basePrice": 150.00,
                                  "currency": "MXN",
                                  "sku": "SKU-001",
                                  "categoryId": "%s",
                                  "images": [
                                    {"url": "https://cdn.example.com/img.jpg", "position": 0, "primary": true}
                                  ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(1));
    }

    @Test
    void create_withMissingName_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": "%s", "basePrice": 100.00}
                                """.formatted(STORE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_withMissingBasePrice_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Remera"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingCategoryId_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Remera", "basePrice": 100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_withNegativePrice_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": "%s", "name": "Remera", "basePrice": -10.00}
                                """.formatted(STORE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // --- GET /api/products/{id} ---

    @Test
    void getById_whenProductExists_returns200() throws Exception {
        when(getProductUseCase.getById(any())).thenReturn(buildProductView());

        mvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Remera Básica"))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(getProductUseCase.getById(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/product-not-found"));
    }

    // --- GET /api/products?storeId= ---

    @Test
    void getByStore_returnsProductList() throws Exception {
        when(getProductUseCase.getByStoreId(STORE_ID)).thenReturn(List.of(buildProductView(), buildProductView()));

        mvc.perform(get("/api/products").param("storeId", STORE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- POST /api/products/{id}/variants/{variantId}/stock ---

    @Test
    void addVariantStock_withValidQty_returns200() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(addStockUseCase.addStock(any())).thenReturn(buildVariantView());

        mvc.perform(post("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock.totalQuantity").value(20))
                .andExpect(jsonPath("$.stock.availableQuantity").value(20));
    }

    @Test
    void addVariantStock_withZeroQty_returns400() throws Exception {
        mvc.perform(post("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addVariantStock_whenVariantNotFound_returns404() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(addStockUseCase.addStock(any())).thenThrow(new ProductVariantNotFoundException(VARIANT_ID));

        mvc.perform(post("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 10}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void addVariantStock_whenAccessDenied_returns403() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(addStockUseCase.addStock(any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("not your product"));

        mvc.perform(post("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 10}
                                """))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/products/{id}/images ---

    @Test
    void addImage_withValidRequest_returns200() throws Exception {
        when(addProductImageUseCase.addImage(any())).thenReturn(buildProduct());
        when(getProductUseCase.getById(any())).thenReturn(buildProductView());

        mvc.perform(post("/api/products/{id}/images", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "https://cdn.example.com/img.jpg", "position": 0, "primary": true}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void addImage_withBlankUrl_returns400() throws Exception {
        mvc.perform(post("/api/products/{id}/images", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "", "position": 0, "primary": false}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- PATCH /api/products/{id}/pause ---

    @Test
    void pause_whenSeller_returns204() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        doNothing().when(pauseProductUseCase).pause(any());

        mvc.perform(patch("/api/products/{id}/pause", PRODUCT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void pause_whenProductNotFound_returns404() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(pauseProductUseCase).pause(any());

        mvc.perform(patch("/api/products/{id}/pause", PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/products/{id}/resume ---

    @Test
    void resume_whenSeller_returns204() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        doNothing().when(resumeProductUseCase).resume(any());

        mvc.perform(patch("/api/products/{id}/resume", PRODUCT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void resume_whenProductNotFound_returns404() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(resumeProductUseCase).resume(any());

        mvc.perform(patch("/api/products/{id}/resume", PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/products/{id}/images/batch ---

    @Test
    void addImagesBatch_withValidBody_returns200WithProductView() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(addProductImagesUseCase.addImages(any())).thenReturn(buildProduct());
        when(getProductUseCase.getById(any())).thenReturn(buildProductView());

        mvc.perform(post("/api/products/{id}/images/batch", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images": [
                                    {"url": "https://cdn.example.com/1.jpg", "position": 0, "primary": true},
                                    {"url": "https://cdn.example.com/2.jpg", "position": 1, "primary": false}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Remera Básica"));
    }

    @Test
    void addImagesBatch_withEmptyImages_returns400() throws Exception {
        mvc.perform(post("/api/products/{id}/images/batch", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"images": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- PATCH /api/products/{id}/variants/{variantId}/stock ---

    @Test
    void correctVariantStock_withValidQty_returns200AndForwardsAvailableQuantity() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(correctStockUseCase.correctStock(any())).thenReturn(buildVariantView());

        mvc.perform(patch("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availableQuantity": 8}
                                """))
                .andExpect(status().isOk());

        var commandCaptor = ArgumentCaptor.forClass(CorrectStockUseCase.CorrectStockCommand.class);
        verify(correctStockUseCase).correctStock(commandCaptor.capture());
        assertThat(commandCaptor.getValue().variantId()).isEqualTo(VARIANT_ID);
        assertThat(commandCaptor.getValue().storeId()).isEqualTo(STORE_ID);
        assertThat(commandCaptor.getValue().availableQuantity()).isEqualTo(8);
    }

    @Test
    void correctVariantStock_withNegativeValue_returns400() throws Exception {
        mvc.perform(patch("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availableQuantity": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void correctVariantStock_whenAccessDenied_returns403() throws Exception {
        when(getStoreUseCase.getStoreIdByUserId(any())).thenReturn(STORE_ID);
        when(correctStockUseCase.correctStock(any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("not your product"));

        mvc.perform(patch("/api/products/{id}/variants/{variantId}/stock", PRODUCT_ID, VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availableQuantity": 8}
                                """))
                .andExpect(status().isForbidden());
    }
}
