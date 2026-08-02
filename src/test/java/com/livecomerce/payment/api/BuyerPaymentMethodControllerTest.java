package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateSetupIntentUseCase;
import com.livecomerce.payment.application.port.in.DeleteBuyerPaymentMethodUseCase;
import com.livecomerce.payment.application.port.in.GetBuyerPaymentMethodsUseCase;
import com.livecomerce.payment.application.port.in.SetDefaultBuyerPaymentMethodUseCase;
import com.livecomerce.payment.domain.BuyerPaymentMethod;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = BuyerPaymentMethodController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(BuyerPaymentMethodControllerTest.SecurityResolverConfig.class)
class BuyerPaymentMethodControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean GetBuyerPaymentMethodsUseCase getBuyerPaymentMethodsUseCase;
    @MockitoBean SetDefaultBuyerPaymentMethodUseCase setDefaultBuyerPaymentMethodUseCase;
    @MockitoBean DeleteBuyerPaymentMethodUseCase deleteBuyerPaymentMethodUseCase;
    @MockitoBean CreateSetupIntentUseCase createSetupIntentUseCase;

    private static final UUID USER_ID           = UUID.randomUUID();
    private static final UUID PAYMENT_METHOD_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                USER_ID, "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private BuyerPaymentMethod buildPaymentMethod() {
        return BuyerPaymentMethod.create(USER_ID, "pm_123", "visa", "4242", (short) 12, (short) 2030);
    }

    // --- POST /api/buyer/payment-methods/setup-intent ---

    @Test
    void createSetupIntent_returns200WithClientSecret() throws Exception {
        when(createSetupIntentUseCase.createSetupIntent(any()))
                .thenReturn(new CreateSetupIntentUseCase.SetupIntentResult("seti_123_secret_abc"));

        mvc.perform(post("/api/buyer/payment-methods/setup-intent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("seti_123_secret_abc"));

        verify(createSetupIntentUseCase).createSetupIntent(
                argThat(cmd -> "buyer@test.com".equals(cmd.email())));
    }

    @Test
    void createSetupIntent_whenBuyerHasNoEmail_doesNotSendPhoneAsEmail() throws Exception {
        var phoneOnlyPrincipal = new UserPrincipal(
                USER_ID, "+15551234567", null, "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phoneOnlyPrincipal, null, phoneOnlyPrincipal.getAuthorities())
        );

        when(createSetupIntentUseCase.createSetupIntent(any()))
                .thenReturn(new CreateSetupIntentUseCase.SetupIntentResult("seti_456_secret_def"));

        mvc.perform(post("/api/buyer/payment-methods/setup-intent"))
                .andExpect(status().isOk());

        verify(createSetupIntentUseCase).createSetupIntent(
                argThat(cmd -> cmd.email() == null));
    }

    // --- GET /api/buyer/payment-methods ---

    @Test
    void getPaymentMethods_returns200WithList() throws Exception {
        when(getBuyerPaymentMethodsUseCase.listByUserId(USER_ID)).thenReturn(List.of(buildPaymentMethod()));

        mvc.perform(get("/api/buyer/payment-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("visa"))
                .andExpect(jsonPath("$[0].last4").value("4242"));
    }

    // --- PATCH /api/buyer/payment-methods/{id}/default ---

    @Test
    void setDefault_returns204() throws Exception {
        doNothing().when(setDefaultBuyerPaymentMethodUseCase).setDefault(USER_ID, PAYMENT_METHOD_ID);

        mvc.perform(patch("/api/buyer/payment-methods/{id}/default", PAYMENT_METHOD_ID))
                .andExpect(status().isNoContent());
    }

    // --- DELETE /api/buyer/payment-methods/{id} ---

    @Test
    void deletePaymentMethod_returns204() throws Exception {
        doNothing().when(deleteBuyerPaymentMethodUseCase).delete(eq(USER_ID), eq(PAYMENT_METHOD_ID));

        mvc.perform(delete("/api/buyer/payment-methods/{id}", PAYMENT_METHOD_ID))
                .andExpect(status().isNoContent());
    }
}
