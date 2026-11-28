package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.*;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.domain.CatalogProductNotAvailableException;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.SellerStoreNotOpenException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveProduct;
import com.livecomerce.live.domain.LiveProductNotFoundException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveProductController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(LiveProductControllerTest.SecurityResolverConfig.class)
class LiveProductControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean AddCatalogProductUseCase addCatalogProductUseCase;
    @MockitoBean AddHotProductUseCase addHotProductUseCase;
    @MockitoBean PinProductUseCase pinProductUseCase;
    @MockitoBean UnpinProductUseCase unpinProductUseCase;
    @MockitoBean ReorderProductsUseCase reorderProductsUseCase;
    @MockitoBean ExpirePinUseCase expirePinUseCase;
    @MockitoBean LoadLiveProductPort loadLiveProductPort;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID LIVE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        setPrincipal(SELLER_ID, "ROLE_SELLER");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Live buildLive() {
        return Live.create(SELLER_ID, "Test Live", null, null, 60);
    }

    private static LiveProduct buildCatalogProduct() {
        return LiveProduct.forCatalogProduct(
                buildLive(),
                UUID.randomUUID(), UUID.randomUUID(),
                "Test Product", new BigDecimal("99.00"), "MXN",
                10, "https://cdn.test/product.jpg"
        );
    }

    private static LiveProduct buildHotProduct() {
        return LiveProduct.forHotProduct(
                buildLive(), "Hot Deal", new BigDecimal("49.00"), "MXN", 5, null
        );
    }

    // --- POST /api/lives/{liveId}/products/catalog ---

    @Test
    void addCatalogProduct_asSeller_returns201() throws Exception {
        when(addCatalogProductUseCase.addCatalogProduct(any())).thenReturn(buildCatalogProduct());
        var productId = UUID.randomUUID();
        var variantId = UUID.randomUUID();

        mvc.perform(post("/api/lives/{liveId}/products/catalog", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "variantId": "%s",
                                  "stockAllocated": 10,
                                  "imageUrl": "https://cdn.test/product.jpg"
                                }
                                """.formatted(productId, variantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productNameSnapshot").value("Test Product"))
                .andExpect(jsonPath("$.isHot").value(false))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.test/product.jpg"));

        var captor = org.mockito.ArgumentCaptor.forClass(AddCatalogProductUseCase.AddCatalogProductCommand.class);
        verify(addCatalogProductUseCase).addCatalogProduct(captor.capture());
        var command = captor.getValue();
        assertThat(command.liveId()).isEqualTo(LIVE_ID);
        assertThat(command.sellerId()).isEqualTo(SELLER_ID);
        assertThat(command.productId()).isEqualTo(productId);
        assertThat(command.variantId()).isEqualTo(variantId);
        assertThat(command.stockAllocated()).isEqualTo(10);
        assertThat(command.imageUrl()).isEqualTo("https://cdn.test/product.jpg");
    }

    @Test
    void addCatalogProduct_legacyClientSendingSnapshotFields_isAcceptedAndFieldsIgnored() throws Exception {
        when(addCatalogProductUseCase.addCatalogProduct(any())).thenReturn(buildCatalogProduct());

        // Old clients still send name/price/currency; they must not break the request,
        // and the use case never receives them (the snapshot comes from the catalog).
        mvc.perform(post("/api/lives/{liveId}/products/catalog", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "variantId": "%s",
                                  "nameSnapshot": "Forged Name",
                                  "priceSnapshot": 0.01,
                                  "currency": "USD",
                                  "stockAllocated": 10
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                // The use case (stubbed with catalog values) decides the snapshot, not the client.
                .andExpect(jsonPath("$.productNameSnapshot").value("Test Product"))
                .andExpect(jsonPath("$.priceSnapshot").value(99.00))
                .andExpect(jsonPath("$.currencySnapshot").value("MXN"));

        var captor = org.mockito.ArgumentCaptor.forClass(AddCatalogProductUseCase.AddCatalogProductCommand.class);
        verify(addCatalogProductUseCase).addCatalogProduct(captor.capture());
        assertThat(captor.getValue().stockAllocated()).isEqualTo(10);
        assertThat(captor.getValue().toString()).doesNotContain("Forged Name").doesNotContain("USD");
    }

    @Test
    void addCatalogProduct_sellerWithoutOpenStore_returns409() throws Exception {
        when(addCatalogProductUseCase.addCatalogProduct(any()))
                .thenThrow(new SellerStoreNotOpenException(SELLER_ID));

        mvc.perform(post("/api/lives/{liveId}/products/catalog", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": "%s", "variantId": "%s", "stockAllocated": 1}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/seller-store-not-open"));
    }

    @Test
    void addCatalogProduct_productNotInSellersStore_returns422() throws Exception {
        when(addCatalogProductUseCase.addCatalogProduct(any()))
                .thenThrow(CatalogProductNotAvailableException.notInSellerStore(PRODUCT_ID, UUID.randomUUID()));

        mvc.perform(post("/api/lives/{liveId}/products/catalog", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": "%s", "variantId": "%s", "stockAllocated": 1}
                                """.formatted(PRODUCT_ID, UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/catalog-product-not-available"));
    }

    @Test
    void addCatalogProduct_withMissingFields_returns400() throws Exception {
        mvc.perform(post("/api/lives/{liveId}/products/catalog", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/lives/{liveId}/products/hot ---

    @Test
    void addHotProduct_asSeller_returns201() throws Exception {
        when(addHotProductUseCase.addHotProduct(any())).thenReturn(buildHotProduct());

        mvc.perform(post("/api/lives/{liveId}/products/hot", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hot Deal",
                                  "price": 49.00,
                                  "currency": "MXN",
                                  "stockAllocated": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productNameSnapshot").value("Hot Deal"))
                .andExpect(jsonPath("$.isHot").value(true));
    }

    // --- POST /api/lives/{liveId}/products/{id}/pin ---

    @Test
    void pinProduct_asSeller_returns200() throws Exception {
        var product = buildCatalogProduct();
        when(pinProductUseCase.pinProduct(any())).thenReturn(product);

        mvc.perform(post("/api/lives/{liveId}/products/{id}/pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isOk());
    }

    // --- DELETE /api/lives/{liveId}/products/{id}/pin ---

    @Test
    void unpinProduct_asSeller_returns200() throws Exception {
        var product = buildCatalogProduct();
        when(unpinProductUseCase.unpinProduct(any())).thenReturn(product);

        mvc.perform(delete("/api/lives/{liveId}/products/{id}/pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isOk());
    }

    // --- PUT /api/lives/{liveId}/products/reorder ---

    @Test
    void reorderProducts_asSeller_returns200() throws Exception {
        var p1 = buildCatalogProduct();
        var p2 = buildHotProduct();
        when(reorderProductsUseCase.reorderProducts(any())).thenReturn(List.of(p1, p2));

        mvc.perform(put("/api/lives/{liveId}/products/reorder", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedProductIds": ["%s", "%s"]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void reorderProducts_withNullList_returns400() throws Exception {
        mvc.perform(put("/api/lives/{liveId}/products/reorder", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/lives/{liveId}/products ---

    @Test
    void listProducts_returns200WithList() throws Exception {
        when(loadLiveProductPort.loadByLiveId(LIVE_ID))
                .thenReturn(List.of(buildCatalogProduct(), buildHotProduct()));

        mvc.perform(get("/api/lives/{liveId}/products", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productNameSnapshot").exists());
    }

    // --- POST /api/lives/{liveId}/products/{id}/expire-pin ---

    @Test
    void expirePin_stockExhausted_returns200WithSoldStatus() throws Exception {
        var product = buildCatalogProduct();
        product.pin();
        product.incrementStockSold(); // sold = 10 out of 10, but we need to exhaust
        // Use a product that when expired becomes SOLD
        when(expirePinUseCase.expirePin(any())).thenAnswer(inv -> {
            var lp = buildCatalogProduct();
            lp.pin();
            lp.markAsSold();
            return lp;
        });

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    void expirePin_stockRemaining_returns200WithAvailableStatus() throws Exception {
        when(expirePinUseCase.expirePin(any())).thenReturn(buildCatalogProduct());

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void expirePin_alreadyAvailable_returns200Idempotent() throws Exception {
        when(expirePinUseCase.expirePin(any())).thenReturn(buildCatalogProduct());

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void expirePin_nonOwnerSeller_returns403() throws Exception {
        when(expirePinUseCase.expirePin(any()))
                .thenThrow(new LiveNotOwnedBySellerException(LIVE_ID, UUID.randomUUID()));

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void expirePin_liveNotFound_returns404() throws Exception {
        when(expirePinUseCase.expirePin(any()))
                .thenThrow(new LiveNotFoundException(LIVE_ID));

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void expirePin_productNotFound_returns404() throws Exception {
        when(expirePinUseCase.expirePin(any()))
                .thenThrow(new LiveProductNotFoundException(PRODUCT_ID));

        mvc.perform(post("/api/lives/{liveId}/products/{id}/expire-pin", LIVE_ID, PRODUCT_ID))
                .andExpect(status().isNotFound());
    }
}
