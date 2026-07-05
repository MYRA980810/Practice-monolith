package com.livecomerce.analytics.api;

import com.livecomerce.analytics.application.LiveSummaryNotFoundException;
import com.livecomerce.analytics.application.LiveSummaryNotOwnedException;
import com.livecomerce.analytics.application.port.in.GetChannelMetricsUseCase;
import com.livecomerce.analytics.application.port.in.GetDeadStockUseCase;
import com.livecomerce.analytics.application.port.in.GetLiveSummaryUseCase;
import com.livecomerce.analytics.application.port.in.GetProductRotationUseCase;
import com.livecomerce.analytics.application.port.in.GetSalesMetricsUseCase;
import com.livecomerce.analytics.domain.LiveSummary;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = AnalyticsController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(AnalyticsControllerTest.SecurityResolverConfig.class)
class AnalyticsControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean GetStoreUseCase getStoreUseCase;
    @MockitoBean GetSalesMetricsUseCase getSalesMetricsUseCase;
    @MockitoBean GetChannelMetricsUseCase getChannelMetricsUseCase;
    @MockitoBean GetProductRotationUseCase getProductRotationUseCase;
    @MockitoBean GetDeadStockUseCase getDeadStockUseCase;
    @MockitoBean GetLiveSummaryUseCase getLiveSummaryUseCase;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID LIVE_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                SELLER_ID, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(getStoreUseCase.getStoreIdByUserId(SELLER_ID)).thenReturn(STORE_ID);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static LiveSummary buildSummary() {
        var summary = LiveSummary.create(
                LIVE_ID, SELLER_ID, STORE_ID,
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(),
                3600, 42);
        summary.addOrder(UUID.randomUUID(), BUYER_ID, "Product A, Product B", new BigDecimal("150.00"));
        summary.finalizeTotals(new BigDecimal("150.00"), 1);
        return summary;
    }

    // --- GET /api/analytics/lives/{liveId}/summary ---

    @Test
    void getLiveSummary_owner_returns200WithSummary() throws Exception {
        when(getLiveSummaryUseCase.getSummary(LIVE_ID, STORE_ID)).thenReturn(buildSummary());

        mvc.perform(get("/api/analytics/lives/{liveId}/summary", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveId").value(LIVE_ID.toString()))
                .andExpect(jsonPath("$.durationSeconds").value(3600))
                .andExpect(jsonPath("$.peakViewers").value(42))
                .andExpect(jsonPath("$.totalSales").value(150.00))
                .andExpect(jsonPath("$.orderCount").value(1))
                .andExpect(jsonPath("$.orders[0].buyerId").value(BUYER_ID.toString()))
                .andExpect(jsonPath("$.orders[0].itemNames").value("Product A, Product B"))
                .andExpect(jsonPath("$.orders[0].orderTotal").value(150.00));
    }

    @Test
    void getLiveSummary_absent_returns404() throws Exception {
        when(getLiveSummaryUseCase.getSummary(any(), any()))
                .thenThrow(new LiveSummaryNotFoundException(LIVE_ID));

        mvc.perform(get("/api/analytics/lives/{liveId}/summary", LIVE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLiveSummary_nonOwner_returns403() throws Exception {
        when(getLiveSummaryUseCase.getSummary(any(), any()))
                .thenThrow(new LiveSummaryNotOwnedException(LIVE_ID, STORE_ID));

        mvc.perform(get("/api/analytics/lives/{liveId}/summary", LIVE_ID))
                .andExpect(status().isForbidden());
    }
}
