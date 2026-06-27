package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.AddBuyerAddressUseCase;
import com.livecomerce.store.application.port.in.DeleteBuyerAddressUseCase;
import com.livecomerce.store.application.port.in.GetBuyerAddressesUseCase;
import com.livecomerce.store.application.port.in.SetDefaultBuyerAddressUseCase;
import com.livecomerce.store.domain.BuyerAddress;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = BuyerAddressController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(BuyerAddressControllerTest.SecurityResolverConfig.class)
class BuyerAddressControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean AddBuyerAddressUseCase addBuyerAddressUseCase;
    @MockitoBean GetBuyerAddressesUseCase getBuyerAddressesUseCase;
    @MockitoBean SetDefaultBuyerAddressUseCase setDefaultBuyerAddressUseCase;
    @MockitoBean DeleteBuyerAddressUseCase deleteBuyerAddressUseCase;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

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

    private BuyerAddress buildAddress() {
        return BuyerAddress.create(USER_ID, "Av. Corrientes", "1234", null,
                "San Nicolás", "Buenos Aires", "CABA", "C1043", "AR");
    }

    // --- POST /api/buyer/addresses ---

    @Test
    void addAddress_withValidRequest_returns201() throws Exception {
        when(addBuyerAddressUseCase.add(any())).thenReturn(buildAddress());

        mvc.perform(post("/api/buyer/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "street": "Av. Corrientes",
                                  "extNumber": "1234",
                                  "city": "Buenos Aires",
                                  "state": "CABA",
                                  "zipCode": "C1043",
                                  "country": "AR",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("Av. Corrientes"))
                .andExpect(jsonPath("$.city").value("Buenos Aires"));
    }

    @Test
    void addAddress_withMissingRequiredField_returns400() throws Exception {
        mvc.perform(post("/api/buyer/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extNumber": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/buyer/addresses ---

    @Test
    void getAddresses_returns200WithList() throws Exception {
        when(getBuyerAddressesUseCase.listByUserId(USER_ID)).thenReturn(List.of(buildAddress()));

        mvc.perform(get("/api/buyer/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("Av. Corrientes"));
    }

    // --- PATCH /api/buyer/addresses/{id}/default ---

    @Test
    void setDefault_returns204() throws Exception {
        doNothing().when(setDefaultBuyerAddressUseCase).setDefault(USER_ID, ADDRESS_ID);

        mvc.perform(patch("/api/buyer/addresses/{id}/default", ADDRESS_ID))
                .andExpect(status().isNoContent());
    }

    // --- DELETE /api/buyer/addresses/{id} ---

    @Test
    void deleteAddress_returns204() throws Exception {
        doNothing().when(deleteBuyerAddressUseCase).delete(eq(USER_ID), eq(ADDRESS_ID));

        mvc.perform(delete("/api/buyer/addresses/{id}", ADDRESS_ID))
                .andExpect(status().isNoContent());
    }
}
