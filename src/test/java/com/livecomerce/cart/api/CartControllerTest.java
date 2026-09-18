package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.AddToCartUseCase;
import com.livecomerce.cart.application.port.in.AddToCartUseCase.AddToCartResult;
import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase;
import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase.ChangeQuantityResult;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.CartLineView;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.CombinedCartView;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.StoreCartView;
import com.livecomerce.cart.application.port.in.RemoveFromCartUseCase;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = CartController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(CartControllerTest.SecurityResolverConfig.class)
class CartControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean AddToCartUseCase addToCartUseCase;
    @MockitoBean ChangeQuantityUseCase changeQuantityUseCase;
    @MockitoBean RemoveFromCartUseCase removeFromCartUseCase;
    @MockitoBean GetCombinedCartViewUseCase getCombinedCartViewUseCase;
    @MockitoBean CheckoutCartUseCase checkoutCartUseCase;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                BUYER_ID, "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- POST /api/cart/stores/{storeId}/items ---

    @Test
    void addToCart_accepted_returns201() throws Exception {
        when(addToCartUseCase.addToCart(any())).thenReturn(AddToCartResult.accepted());

        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","variantId":"%s","quantity":2}
                                """.formatted(PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        verify(addToCartUseCase).addToCart(new AddToCartUseCase.AddToCartCommand(
                BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2));
    }

    @Test
    void addToCart_liveExclusive_returns409() throws Exception {
        when(addToCartUseCase.addToCart(any())).thenReturn(AddToCartResult.rejected("LIVE_EXCLUSIVE"));

        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":1}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.rejectionReason").value("LIVE_EXCLUSIVE"));
    }

    @Test
    void addToCart_unavailable_returns409() throws Exception {
        when(addToCartUseCase.addToCart(any())).thenReturn(AddToCartResult.rejected("UNAVAILABLE"));

        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":1}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rejectionReason").value("UNAVAILABLE"));
    }

    @Test
    void addToCart_quantityLimitExceeded_returns400() throws Exception {
        when(addToCartUseCase.addToCart(any())).thenReturn(AddToCartResult.rejected("QUANTITY_LIMIT_EXCEEDED"));

        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":50}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rejectionReason").value("QUANTITY_LIMIT_EXCEEDED"));
    }

    @Test
    void addToCart_missingFields_returns400() throws Exception {
        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_quantityAboveRequestBound_returns400WithoutInvokingUseCase() throws Exception {
        mvc.perform(post("/api/cart/stores/{storeId}/items", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":100}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isBadRequest());

        verify(addToCartUseCase, never()).addToCart(any());
    }

    // --- POST /api/cart/stores/{storeId}/items/{productId}/increment ---

    @Test
    void increment_success_returns200WithResultingQuantity() throws Exception {
        when(changeQuantityUseCase.changeQuantity(any())).thenReturn(ChangeQuantityResult.success(3));

        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/increment", STORE_ID, PRODUCT_ID)
                        .param("variantId", VARIANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultingQuantity").value(3));

        verify(changeQuantityUseCase).changeQuantity(new ChangeQuantityUseCase.ChangeQuantityCommand(
                BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));
    }

    @Test
    void increment_insufficientStock_returns409WithAvailableStock() throws Exception {
        when(changeQuantityUseCase.changeQuantity(any())).thenReturn(ChangeQuantityResult.insufficientStock(8));

        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/increment", STORE_ID, PRODUCT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.failureReason").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.availableStock").value(8));
    }

    @Test
    void increment_lineNotFound_returns404() throws Exception {
        when(changeQuantityUseCase.changeQuantity(any())).thenReturn(ChangeQuantityResult.failure("LINE_NOT_FOUND"));

        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/increment", STORE_ID, PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void increment_deltaAboveRequestBound_returns400WithoutInvokingUseCase() throws Exception {
        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/increment", STORE_ID, PRODUCT_ID)
                        .param("delta", "1000"))
                .andExpect(status().isBadRequest());

        verify(changeQuantityUseCase, never()).changeQuantity(any());
    }

    // --- POST /api/cart/stores/{storeId}/items/{productId}/decrement ---

    @Test
    void decrement_success_negatesDeltaAndReturns200() throws Exception {
        when(changeQuantityUseCase.changeQuantity(any())).thenReturn(ChangeQuantityResult.success(1));

        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/decrement", STORE_ID, PRODUCT_ID)
                        .param("variantId", VARIANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultingQuantity").value(1));

        verify(changeQuantityUseCase).changeQuantity(argThat(cmd -> cmd.delta() == -1
                && cmd.buyerId().equals(BUYER_ID)
                && cmd.productId().equals(PRODUCT_ID)
                && cmd.variantId().equals(VARIANT_ID)));
    }

    @Test
    void decrement_toZero_returns200WithZeroQuantity() throws Exception {
        when(changeQuantityUseCase.changeQuantity(any())).thenReturn(ChangeQuantityResult.success(0));

        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/decrement", STORE_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultingQuantity").value(0));
    }

    @Test
    void decrement_deltaAboveRequestBound_returns400WithoutInvokingUseCase() throws Exception {
        mvc.perform(post("/api/cart/stores/{storeId}/items/{productId}/decrement", STORE_ID, PRODUCT_ID)
                        .param("delta", "1000"))
                .andExpect(status().isBadRequest());

        verify(changeQuantityUseCase, never()).changeQuantity(any());
    }

    // --- DELETE /api/cart/stores/{storeId}/items/{productId} ---

    @Test
    void removeLine_returns204() throws Exception {
        mvc.perform(delete("/api/cart/stores/{storeId}/items/{productId}", STORE_ID, PRODUCT_ID)
                        .param("variantId", VARIANT_ID.toString()))
                .andExpect(status().isNoContent());

        verify(removeFromCartUseCase).removeLine(new RemoveFromCartUseCase.RemoveFromCartCommand(
                BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID));
    }

    @Test
    void removeLine_idempotent_stillReturns204() throws Exception {
        mvc.perform(delete("/api/cart/stores/{storeId}/items/{productId}", STORE_ID, PRODUCT_ID))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/cart ---

    @Test
    void getCombinedView_multipleStores_returns200WithGroups() throws Exception {
        var line = new CartLineView(PRODUCT_ID, VARIANT_ID, "Playera", "https://cdn/img.jpg",
                new BigDecimal("199.00"), "MXN", 2, 10, null);
        var storeView = new StoreCartView(STORE_ID, List.of(line));
        when(getCombinedCartViewUseCase.getCombinedView(BUYER_ID))
                .thenReturn(new CombinedCartView(List.of(storeView)));

        mvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores[0].storeId").value(STORE_ID.toString()))
                .andExpect(jsonPath("$.stores[0].lines[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.stores[0].lines[0].name").value("Playera"))
                .andExpect(jsonPath("$.stores[0].lines[0].quantity").value(2));
    }

    @Test
    void getCombinedView_empty_returns200WithEmptyList() throws Exception {
        when(getCombinedCartViewUseCase.getCombinedView(BUYER_ID)).thenReturn(new CombinedCartView(List.of()));

        mvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores").isArray())
                .andExpect(jsonPath("$.stores").isEmpty());
    }

    @Test
    void getCombinedView_blockedLine_exposesBlockedReason() throws Exception {
        var line = new CartLineView(PRODUCT_ID, null, "Playera", "https://cdn/img.jpg",
                new BigDecimal("199.00"), "MXN", 1, 10, "LIVE_EXCLUSIVE");
        when(getCombinedCartViewUseCase.getCombinedView(BUYER_ID))
                .thenReturn(new CombinedCartView(List.of(new StoreCartView(STORE_ID, List.of(line)))));

        mvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores[0].lines[0].blockedReason").value("LIVE_EXCLUSIVE"));
    }

    // --- POST /api/cart/checkout ---

    @Test
    void checkout_partialSuccess_returns200() throws Exception {
        var skipped = new SkippedLine(UUID.randomUUID(), null, "OUT_OF_STOCK");
        var result = new CheckoutCartUseCase.StoreCheckoutResult(
                STORE_ID, true, UUID.randomUUID(), new BigDecimal("398.00"), "MXN", List.of(skipped), null);
        when(checkoutCartUseCase.checkout(any()))
                .thenReturn(new CheckoutCartUseCase.CheckoutCartResponse(List.of(result)));

        mvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedItems":[{"storeId":"%s","productId":"%s","variantId":"%s"}]}
                                """.formatted(STORE_ID, PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].succeeded").value(true))
                .andExpect(jsonPath("$.results[0].skippedLines[0].reason").value("OUT_OF_STOCK"));
    }

    @Test
    void checkout_totalFailure_stillReturns200() throws Exception {
        var skipped = new SkippedLine(PRODUCT_ID, VARIANT_ID, "OUT_OF_STOCK");
        var result = new CheckoutCartUseCase.StoreCheckoutResult(
                STORE_ID, false, null, null, null, List.of(skipped), "ALL_ITEMS_UNAVAILABLE");
        when(checkoutCartUseCase.checkout(any()))
                .thenReturn(new CheckoutCartUseCase.CheckoutCartResponse(List.of(result)));

        mvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedItems":[{"storeId":"%s","productId":"%s","variantId":"%s"}]}
                                """.formatted(STORE_ID, PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].succeeded").value(false))
                .andExpect(jsonPath("$.results[0].failureReason").value("ALL_ITEMS_UNAVAILABLE"));
    }

    @Test
    void checkout_emptySelection_returns400() throws Exception {
        mvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedItems":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- CartExceptionHandler ---

    @Test
    void unexpectedIllegalArgument_returns400() throws Exception {
        when(getCombinedCartViewUseCase.getCombinedView(any()))
                .thenThrow(new IllegalArgumentException("buyerId must not be null"));

        mvc.perform(get("/api/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("buyerId must not be null"));
    }

    @Test
    void cartStorageFailure_returns503WithoutLeakingExceptionDetails() throws Exception {
        when(getCombinedCartViewUseCase.getCombinedView(any()))
                .thenThrow(new DataAccessResourceFailureException("Redis connection refused at 10.0.0.5:6379"));

        mvc.perform(get("/api/cart"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Cart is temporarily unavailable, please retry."))
                .andExpect(jsonPath("$.detail", not(containsString("Redis"))));
    }
}
