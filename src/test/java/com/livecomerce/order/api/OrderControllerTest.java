package com.livecomerce.order.api;

import com.livecomerce.order.application.InvalidOrderStateException;
import com.livecomerce.order.application.OrderNotFoundException;
import com.livecomerce.order.application.port.in.*;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = OrderController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(OrderControllerTest.SecurityResolverConfig.class)
class OrderControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean PlaceOrderItemUseCase placeOrderItemUseCase;
    @MockitoBean FinalizeOrderUseCase finalizeOrderUseCase;
    @MockitoBean GetOrderUseCase getOrderUseCase;
    @MockitoBean ShipOrderUseCase shipOrderUseCase;

    private static final UUID BUYER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID ORDER_ID  = UUID.randomUUID();
    private static final UUID LIVE_ID   = UUID.randomUUID();

    @BeforeEach
    void setUpBuyerPrincipal() {
        setPrincipal(BUYER_ID, "ROLE_BUYER");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Order buildOrder() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        order.addItem(UUID.randomUUID(), UUID.randomUUID(), "Playera Roja", new BigDecimal("199.00"), "MXN", 1, 10);
        return order;
    }

    // --- POST /api/orders/items ---

    @Test
    void placeItem_withValidRequest_returns201WithOrder() throws Exception {
        when(placeOrderItemUseCase.placeItem(any())).thenReturn(buildOrder());

        mvc.perform(post("/api/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId":"%s",
                                  "liveSessionId":"%s",
                                  "productId":"%s",
                                  "variantId":"%s",
                                  "productName":"Playera Roja",
                                  "unitPrice":199.00,
                                  "currency":"MXN",
                                  "quantity":1
                                }
                                """.formatted(STORE_ID, LIVE_ID, UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Playera Roja"));
    }

    @Test
    void placeItem_withMissingFields_returns400() throws Exception {
        mvc.perform(post("/api/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/orders/active ---

    @Test
    void getActive_whenOrderExists_returns200() throws Exception {
        when(getOrderUseCase.getActiveOrder(BUYER_ID, LIVE_ID)).thenReturn(Optional.of(buildOrder()));

        mvc.perform(get("/api/orders/active").param("liveSessionId", LIVE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getActive_whenNoOrder_returns204() throws Exception {
        when(getOrderUseCase.getActiveOrder(any(), any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/orders/active").param("liveSessionId", LIVE_ID.toString()))
                .andExpect(status().isNoContent());
    }

    // --- POST /api/orders/{id}/finalize ---

    @Test
    void finalize_withValidRequest_returns200() throws Exception {
        var finalized = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        when(finalizeOrderUseCase.finalizeOrder(any())).thenReturn(finalized);

        mvc.perform(post("/api/orders/{id}/finalize", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"Calle 123, CDMX"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void finalize_whenOrderAlreadyFinalized_returns409() throws Exception {
        when(finalizeOrderUseCase.finalizeOrder(any()))
                .thenThrow(new InvalidOrderStateException(ORDER_ID, OrderStatus.PAID, "finalize"));

        mvc.perform(post("/api/orders/{id}/finalize", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"Calle 123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/invalid-order-state"));
    }

    // --- GET /api/orders/{id} ---

    @Test
    void getById_whenOrderExists_returns200() throws Exception {
        when(getOrderUseCase.getById(ORDER_ID)).thenReturn(buildOrder());

        mvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(BUYER_ID.toString()));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(getOrderUseCase.getById(ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

        mvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/order-not-found"));
    }

    // --- GET /api/orders?storeId= (SELLER) ---

    @Test
    void getByStore_asSeller_returns200WithList() throws Exception {
        setPrincipal(UUID.randomUUID(), "ROLE_SELLER");
        when(getOrderUseCase.getByStore(STORE_ID)).thenReturn(List.of(buildOrder()));

        mvc.perform(get("/api/orders").param("storeId", STORE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].storeId").value(STORE_ID.toString()));
    }

    // --- POST /api/orders/{id}/ship (SELLER) ---

    @Test
    void ship_asSeller_returns200WithTrackingNumber() throws Exception {
        setPrincipal(UUID.randomUUID(), "ROLE_SELLER");
        var shipped = buildOrder();
        when(shipOrderUseCase.ship(any())).thenReturn(shipped);

        mvc.perform(post("/api/orders/{id}/ship", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"MX123456789"}
                                """))
                .andExpect(status().isOk());
    }
}
