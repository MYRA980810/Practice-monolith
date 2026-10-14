package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.live.StreamTicketPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.mockito.Mockito.mock;

/**
 * Loads the real {@link SecurityConfig} filter chain (with the real {@link RestAuthenticationEntryPoint})
 * into a {@code @WebMvcTest} slice of another module, whose test cannot reference the package-private
 * security beans directly. The JWT collaborators are inert mocks: tests authenticate through
 * spring-security-test request post-processors instead of real tokens.
 * Requires the {@code app.frontend-url} property.
 */
@TestConfiguration
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
public class ProductionSecurityTestConfig {

    @Bean
    JwtService jwtService() {
        return mock(JwtService.class);
    }

    @Bean
    UserDetailsAdapter userDetailsAdapter() {
        return mock(UserDetailsAdapter.class);
    }

    @Bean
    GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler() {
        return mock(GoogleOAuth2SuccessHandler.class);
    }

    @Bean
    CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository() {
        return mock(CookieOAuth2AuthorizationRequestRepository.class);
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return mock(ClientRegistrationRepository.class);
    }

    @Bean
    StreamTicketPort streamTicketPort() {
        return mock(StreamTicketPort.class);
    }
}
