package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.in.CreateEmbeddedOnboardingSessionUseCase;
import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;
import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = SellerPayoutAccountController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SellerPayoutAccountControllerTest.SecurityResolverConfig.class)
class SellerPayoutAccountControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean GetSellerPayoutStatusUseCase getSellerPayoutStatusUseCase;
    @MockitoBean CreateConnectOnboardingLinkUseCase createConnectOnboardingLinkUseCase;
    @MockitoBean CreateEmbeddedOnboardingSessionUseCase createEmbeddedOnboardingSessionUseCase;
    @MockitoBean GetSellerPayoutAccountDetailsUseCase getSellerPayoutAccountDetailsUseCase;
    @MockitoBean ListSellerPayoutsUseCase listSellerPayoutsUseCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                USER_ID, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- POST /api/seller/payout-account/onboarding-link ---

    @Test
    void createOnboardingLink_returns200WithUrl() throws Exception {
        when(createConnectOnboardingLinkUseCase.createOnboardingLink(any()))
                .thenReturn(new CreateConnectOnboardingLinkUseCase.OnboardingLinkResult(
                        "https://connect.stripe.com/setup/onboarding/abc123"));

        mvc.perform(post("/api/seller/payout-account/onboarding-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://connect.stripe.com/setup/onboarding/abc123"));

        verify(createConnectOnboardingLinkUseCase).createOnboardingLink(
                argThat(cmd -> "seller@test.com".equals(cmd.email())));
    }

    @Test
    void createOnboardingLink_whenSellerHasNoEmail_doesNotSendPhoneAsEmail() throws Exception {
        var phoneOnlyPrincipal = new UserPrincipal(
                USER_ID, "+15551234567", null, "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phoneOnlyPrincipal, null, phoneOnlyPrincipal.getAuthorities())
        );

        when(createConnectOnboardingLinkUseCase.createOnboardingLink(any()))
                .thenReturn(new CreateConnectOnboardingLinkUseCase.OnboardingLinkResult(
                        "https://connect.stripe.com/setup/onboarding/abc123"));

        mvc.perform(post("/api/seller/payout-account/onboarding-link"))
                .andExpect(status().isOk());

        verify(createConnectOnboardingLinkUseCase).createOnboardingLink(
                argThat(cmd -> cmd.email() == null));
    }

    // --- POST /api/seller/payout-account/embedded-onboarding-session ---

    @Test
    void createEmbeddedOnboardingSession_returns200WithClientSecret() throws Exception {
        when(createEmbeddedOnboardingSessionUseCase.createEmbeddedOnboardingSession(any()))
                .thenReturn(new CreateEmbeddedOnboardingSessionUseCase.EmbeddedOnboardingSessionResult(
                        "accs_test_123_secret_abc"));

        mvc.perform(post("/api/seller/payout-account/embedded-onboarding-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("accs_test_123_secret_abc"));

        verify(createEmbeddedOnboardingSessionUseCase).createEmbeddedOnboardingSession(
                argThat(cmd -> "seller@test.com".equals(cmd.email())));
    }

    @Test
    void createEmbeddedOnboardingSession_whenSellerHasNoEmail_doesNotSendPhoneAsEmail() throws Exception {
        var phoneOnlyPrincipal = new UserPrincipal(
                USER_ID, "+15551234567", null, "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phoneOnlyPrincipal, null, phoneOnlyPrincipal.getAuthorities())
        );

        when(createEmbeddedOnboardingSessionUseCase.createEmbeddedOnboardingSession(any()))
                .thenReturn(new CreateEmbeddedOnboardingSessionUseCase.EmbeddedOnboardingSessionResult(
                        "accs_test_123_secret_abc"));

        mvc.perform(post("/api/seller/payout-account/embedded-onboarding-session"))
                .andExpect(status().isOk());

        verify(createEmbeddedOnboardingSessionUseCase).createEmbeddedOnboardingSession(
                argThat(cmd -> cmd.email() == null));
    }

    // --- GET /api/seller/payout-account/status ---

    @Test
    void getStatus_returns200WithStatus() throws Exception {
        when(getSellerPayoutStatusUseCase.getStatus(USER_ID))
                .thenReturn(new GetSellerPayoutStatusUseCase.PayoutStatusResult(true, true, true, true));

        mvc.perform(get("/api/seller/payout-account/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargesEnabled").value(true))
                .andExpect(jsonPath("$.payoutsEnabled").value(true))
                .andExpect(jsonPath("$.detailsSubmitted").value(true))
                .andExpect(jsonPath("$.connected").value(true));
    }

    // --- GET /api/seller/payout-account/details ---

    @Test
    void getDetails_returns200WithAccountDetails() throws Exception {
        when(getSellerPayoutAccountDetailsUseCase.getDetails(USER_ID))
                .thenReturn(new GetSellerPayoutAccountDetailsUseCase.PayoutAccountDetailsResult(
                        true, true, true, true,
                        "Acme Store", "https://acme.example.com", "seller@example.com",
                        List.of("individual.verification.document"), List.of(), null,
                        "STRIPE TEST BANK", "6789", "new",
                        List.of(new GetSellerPayoutAccountDetailsUseCase.BalanceAmountResult(1000, "usd")),
                        List.of(new GetSellerPayoutAccountDetailsUseCase.BalanceAmountResult(500, "usd"))
                ));

        mvc.perform(get("/api/seller/payout-account/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.chargesEnabled").value(true))
                .andExpect(jsonPath("$.payoutsEnabled").value(true))
                .andExpect(jsonPath("$.detailsSubmitted").value(true))
                .andExpect(jsonPath("$.businessName").value("Acme Store"))
                .andExpect(jsonPath("$.businessUrl").value("https://acme.example.com"))
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.currentlyDue[0]").value("individual.verification.document"))
                .andExpect(jsonPath("$.pastDue").isEmpty())
                .andExpect(jsonPath("$.disabledReason").doesNotExist())
                .andExpect(jsonPath("$.bankName").value("STRIPE TEST BANK"))
                .andExpect(jsonPath("$.bankLast4").value("6789"))
                .andExpect(jsonPath("$.bankAccountStatus").value("new"))
                .andExpect(jsonPath("$.balanceAvailable[0].amount").value(1000))
                .andExpect(jsonPath("$.balanceAvailable[0].currency").value("usd"))
                .andExpect(jsonPath("$.balancePending[0].amount").value(500))
                .andExpect(jsonPath("$.balancePending[0].currency").value("usd"));
    }

    @Test
    void getDetails_whenNotConnected_returns200WithNotConnectedResult() throws Exception {
        when(getSellerPayoutAccountDetailsUseCase.getDetails(USER_ID))
                .thenReturn(GetSellerPayoutAccountDetailsUseCase.PayoutAccountDetailsResult.notConnected());

        mvc.perform(get("/api/seller/payout-account/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.businessName").doesNotExist())
                .andExpect(jsonPath("$.bankLast4").doesNotExist());
    }

    // --- GET /api/seller/payout-account/payouts ---

    @Test
    void listPayouts_withDefaultLimit_returns200WithPage() throws Exception {
        when(listSellerPayoutsUseCase.listPayouts(USER_ID, null, 10))
                .thenReturn(new ListSellerPayoutsUseCase.PayoutPageResult(
                        List.of(new ListSellerPayoutsUseCase.PayoutItemResult(
                                "po_1", 1000, "usd", "paid", "standard",
                                OffsetDateTime.parse("2026-07-01T00:00:00Z"))),
                        "po_1",
                        true
                ));

        mvc.perform(get("/api/seller/payout-account/payouts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("po_1"))
                .andExpect(jsonPath("$.items[0].amount").value(1000))
                .andExpect(jsonPath("$.items[0].currency").value("usd"))
                .andExpect(jsonPath("$.items[0].status").value("paid"))
                .andExpect(jsonPath("$.items[0].method").value("standard"))
                .andExpect(jsonPath("$.nextCursor").value("po_1"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(listSellerPayoutsUseCase).listPayouts(USER_ID, null, 10);
    }

    @Test
    void listPayouts_withExplicitCursorAndLimit_forwardsThemAsIs() throws Exception {
        when(listSellerPayoutsUseCase.listPayouts(USER_ID, "po_5", 25))
                .thenReturn(ListSellerPayoutsUseCase.PayoutPageResult.empty());

        mvc.perform(get("/api/seller/payout-account/payouts")
                        .param("cursor", "po_5")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(listSellerPayoutsUseCase).listPayouts(USER_ID, "po_5", 25);
    }

    @Test
    void listPayouts_withLimitAboveMax_clampsTo100() throws Exception {
        when(listSellerPayoutsUseCase.listPayouts(USER_ID, null, 100))
                .thenReturn(ListSellerPayoutsUseCase.PayoutPageResult.empty());

        mvc.perform(get("/api/seller/payout-account/payouts")
                        .param("limit", "500"))
                .andExpect(status().isOk());

        verify(listSellerPayoutsUseCase).listPayouts(USER_ID, null, 100);
    }

    @Test
    void listPayouts_withLimitBelowMin_clampsTo1() throws Exception {
        when(listSellerPayoutsUseCase.listPayouts(USER_ID, null, 1))
                .thenReturn(ListSellerPayoutsUseCase.PayoutPageResult.empty());

        mvc.perform(get("/api/seller/payout-account/payouts")
                        .param("limit", "0"))
                .andExpect(status().isOk());

        verify(listSellerPayoutsUseCase).listPayouts(USER_ID, null, 1);
    }
}
