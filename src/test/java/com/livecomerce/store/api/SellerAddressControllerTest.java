package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.AddSellerAddressUseCase;
import com.livecomerce.store.application.port.in.DeleteSellerAddressUseCase;
import com.livecomerce.store.application.port.in.GetSellerAddressesUseCase;
import com.livecomerce.store.application.port.in.SetDefaultSellerAddressUseCase;
import com.livecomerce.store.application.port.in.UpdateSellerAddressUseCase;
import com.livecomerce.store.domain.AddressType;
import com.livecomerce.store.domain.SellerAddress;
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
        controllers = SellerAddressController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SellerAddressControllerTest.SecurityResolverConfig.class)
class SellerAddressControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean AddSellerAddressUseCase addSellerAddressUseCase;
    @MockitoBean UpdateSellerAddressUseCase updateSellerAddressUseCase;
    @MockitoBean GetSellerAddressesUseCase getSellerAddressesUseCase;
    @MockitoBean SetDefaultSellerAddressUseCase setDefaultSellerAddressUseCase;
    @MockitoBean DeleteSellerAddressUseCase deleteSellerAddressUseCase;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

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

    private SellerAddress buildAddress() {
        return SellerAddress.create(USER_ID, "Av. Corrientes", "1234", null,
                "San Nicolás", "Buenos Aires", "CABA", "C1043", "AR", -34.6037, -58.3816,
                AddressType.STORE);
    }

    // --- POST /api/seller/addresses ---

    @Test
    void addAddress_withValidRequest_returns201() throws Exception {
        when(addSellerAddressUseCase.add(any())).thenReturn(buildAddress());

        mvc.perform(post("/api/seller/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "street": "Av. Corrientes",
                                  "extNumber": "1234",
                                  "city": "Buenos Aires",
                                  "state": "CABA",
                                  "zipCode": "C1043",
                                  "country": "AR",
                                  "isDefault": false,
                                  "latitude": -34.6037,
                                  "longitude": -58.3816,
                                  "addressType": "STORE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("Av. Corrientes"))
                .andExpect(jsonPath("$.city").value("Buenos Aires"))
                .andExpect(jsonPath("$.latitude").value(-34.6037))
                .andExpect(jsonPath("$.longitude").value(-58.3816));
    }

    @Test
    void addAddress_withMissingRequiredField_returns400() throws Exception {
        mvc.perform(post("/api/seller/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extNumber": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAddress_withMissingAddressType_returns400() throws Exception {
        mvc.perform(post("/api/seller/addresses")
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAddress_withMissingExtNumber_returns400() throws Exception {
        mvc.perform(post("/api/seller/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "street": "Av. Corrientes",
                                  "city": "Buenos Aires",
                                  "state": "CABA",
                                  "zipCode": "C1043",
                                  "country": "AR",
                                  "isDefault": false,
                                  "addressType": "STORE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAddress_withInvalidAddressType_returns400() throws Exception {
        mvc.perform(post("/api/seller/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "street": "Av. Corrientes",
                                  "city": "Buenos Aires",
                                  "state": "CABA",
                                  "zipCode": "C1043",
                                  "country": "AR",
                                  "isDefault": false,
                                  "addressType": "GARAGE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/seller/addresses/{id} ---

    @Test
    void updateAddress_withValidRequest_returns200() throws Exception {
        when(updateSellerAddressUseCase.update(any())).thenReturn(buildAddress());

        mvc.perform(put("/api/seller/addresses/{id}", ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "street": "Av. Corrientes",
                                  "extNumber": "1234",
                                  "city": "Buenos Aires",
                                  "state": "CABA",
                                  "zipCode": "C1043",
                                  "country": "AR",
                                  "latitude": -34.6037,
                                  "longitude": -58.3816,
                                  "addressType": "STORE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Av. Corrientes"))
                .andExpect(jsonPath("$.city").value("Buenos Aires"));
    }

    @Test
    void updateAddress_withMissingRequiredField_returns400() throws Exception {
        mvc.perform(put("/api/seller/addresses/{id}", ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extNumber": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/seller/addresses ---

    @Test
    void getAddresses_returns200WithList() throws Exception {
        when(getSellerAddressesUseCase.listByUserId(USER_ID)).thenReturn(List.of(buildAddress()));

        mvc.perform(get("/api/seller/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("Av. Corrientes"));
    }

    // --- PATCH /api/seller/addresses/{id}/default ---

    @Test
    void setDefault_returns204() throws Exception {
        doNothing().when(setDefaultSellerAddressUseCase).setDefault(USER_ID, ADDRESS_ID);

        mvc.perform(patch("/api/seller/addresses/{id}/default", ADDRESS_ID))
                .andExpect(status().isNoContent());
    }

    // --- DELETE /api/seller/addresses/{id} ---

    @Test
    void deleteAddress_returns204() throws Exception {
        doNothing().when(deleteSellerAddressUseCase).delete(eq(USER_ID), eq(ADDRESS_ID));

        mvc.perform(delete("/api/seller/addresses/{id}", ADDRESS_ID))
                .andExpect(status().isNoContent());
    }
}
