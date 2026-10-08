package com.livecomerce.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that an authenticated SSE response (e.g. GET /api/lives/feed/stream) survives its
 * async dispatch: the JWT filter only runs on the initial request, so SecurityConfig must permit
 * ASYNC (and ERROR) dispatches or they'd be rejected after the stream started. The stream itself
 * still requires a JWT. Uses a stand-in SSE controller so only the security rule is under test.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = AsyncDispatchSecurityTest.StreamController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, AsyncDispatchSecurityTest.StreamController.class})
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "jwt.secret=livecomerce-secret-key-change-this-in-production-min-32chars",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=2592000000"
})
class AsyncDispatchSecurityTest {

    @RestController
    static class StreamController {
        @GetMapping(path = "/api/lives/feed/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        SseEmitter stream() throws IOException {
            var emitter = new SseEmitter();
            emitter.send(SseEmitter.event().comment("connected"));
            emitter.complete();
            return emitter;
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean JwtService                                 jwtService;
    @MockitoBean UserDetailsAdapter                         userDetailsAdapter;
    @MockitoBean GoogleOAuth2SuccessHandler                 googleOAuth2SuccessHandler;
    @MockitoBean CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    @MockitoBean ClientRegistrationRepository               clientRegistrationRepository;

    @Test
    void stream_withoutJwt_isRejectedWith401() throws Exception {
        mvc.perform(get("/api/lives/feed/stream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stream_withJwt_isNotRejectedOnItsAsyncDispatch() throws Exception {
        var claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(jwtService.validateAndExtract("valid")).thenReturn(claims);
        when(userDetailsAdapter.loadUserByUsername("user-1"))
                .thenReturn(User.withUsername("user-1").password("n/a").roles("BUYER").build());

        var started = mvc.perform(get("/api/lives/feed/stream").header("Authorization", "Bearer valid"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk());
    }
}
