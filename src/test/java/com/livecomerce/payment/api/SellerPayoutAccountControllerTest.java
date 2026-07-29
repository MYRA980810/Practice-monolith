package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
}
