package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.BuyLiveProductUseCase;
import com.livecomerce.live.application.port.in.ExitLiveUseCase;
import com.livecomerce.live.application.port.in.ExitLiveUseCase.ExitLiveResponse;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveBuyController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(LiveBuyControllerTest.SecurityResolverConfig.class)
class LiveBuyControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean BuyLiveProductUseCase buyLiveProductUseCase;
    @MockitoBean ExitLiveUseCase exitLiveUseCase;

    private static final UUID BUYER_ID        = UUID.randomUUID();
    private static final UUID STORE_ID        = UUID.randomUUID();
    private static final UUID LIVE_ID         = UUID.randomUUID();
    private static final UUID LIVE_PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        setPrincipal(BUYER_ID, "ROLE_BUYER");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Order buildOrder() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        order.addItem(UUID.randomUUID(), UUID.randomUUID(), "Product", new BigDecimal("99.00"), "MXN", 1, 10, OrderItemType.HOT_PRODUCT);
        return order;
    }

    // --- POST /api/lives/{liveId}/buy ---

    @Test
    void buy_asBuyerWithValidRequest_returns201WithOrder() throws Exception {
        when(buyLiveProductUseCase.buyLiveProduct(any())).thenReturn(buildOrder());

        mvc.perform(post("/api/lives/{liveId}/buy", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "liveProductId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(LIVE_PRODUCT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buyerId").value(BUYER_ID.toString()));
    }

    @Test
    void buy_withMissingFields_returns400() throws Exception {
        mvc.perform(post("/api/lives/{liveId}/buy", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/lives/{liveId}/exit ---

    @Test
    void exit_withActiveOrder_returns200WithOrder() throws Exception {
        var order    = buildOrder();
        var response = new ExitLiveResponse(order, null, null);
        when(exitLiveUseCase.exitLive(any())).thenReturn(response);

        mvc.perform(post("/api/lives/{liveId}/exit", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress": "Calle 123, CDMX"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.buyerId").value(BUYER_ID.toString()));
    }

    @Test
    void exit_withNoActiveOrder_returns204() throws Exception {
        var response = new ExitLiveResponse(null, null, null);
        when(exitLiveUseCase.exitLive(any())).thenReturn(response);

        mvc.perform(post("/api/lives/{liveId}/exit", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }
}
