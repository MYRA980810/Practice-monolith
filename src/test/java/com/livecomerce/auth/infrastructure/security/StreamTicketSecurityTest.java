package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.live.StreamTicketPort;
import com.livecomerce.shared.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stream-ticket authentication of the SSE lives feed against the real security filter chain:
 * {@code X-Stream-Ticket} authenticates only {@code GET /api/lives/feed/stream}, is redeemed once
 * (never again on the async dispatch), maps a bad ticket to 401 {@code TICKET_INVALID}, and leaves
 * Bearer authentication working. Also covers the stream's dedicated CORS preflight.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = StreamTicketSecurityTest.StandInControllers.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, StreamTicketSecurityTest.StandInControllers.class})
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.feed-stream.allowed-origin-patterns=https://livecomerce.app, https://livecomerce-web-*-acme.vercel.app",
        "jwt.secret=livecomerce-secret-key-change-this-in-production-min-32chars",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=2592000000"
})
class StreamTicketSecurityTest {

    private static final String STREAM = "/api/lives/feed/stream";
    private static final String TICKET_HEADER = "X-Stream-Ticket";

    /** Stand-ins for the live endpoints, so only the security behaviour is under test. */
    @RestController
    static class StandInControllers {
        @PreAuthorize("hasRole('BUYER')")
        @GetMapping(path = STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) throws IOException {
            var emitter = new SseEmitter();
            emitter.send(SseEmitter.event().comment("user=" + principal.getUserId()));
            emitter.complete();
            return emitter;
        }

        @PostMapping(STREAM)
        String postToStreamPath() {
            return "ok";
        }

        @GetMapping("/api/lives/active")
        String otherEndpoint() {
            return "ok";
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean StreamTicketPort                           streamTicketPort;
    @MockitoBean JwtService                                 jwtService;
    @MockitoBean UserDetailsAdapter                         userDetailsAdapter;
    @MockitoBean GoogleOAuth2SuccessHandler                 googleOAuth2SuccessHandler;
    @MockitoBean CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    @MockitoBean ClientRegistrationRepository               clientRegistrationRepository;

    private UUID givenValidTicket(String ticket, String role) {
        var userId = UUID.randomUUID();
        when(streamTicketPort.consume(ticket)).thenReturn(Optional.of(userId));
        when(userDetailsAdapter.loadUserByUsername(userId.toString()))
                .thenReturn(new UserPrincipal(userId, "u@test.com", "hash",
                        List.of(new SimpleGrantedAuthority(role)), true));
        return userId;
    }

    // --- ticket authentication ---

    @Test
    void validBuyerTicket_opensTheStreamAsThatUser_andIsRedeemedOnlyOnceAcrossTheAsyncDispatch() throws Exception {
        var userId = givenValidTicket("good", "ROLE_BUYER");

        var started = mvc.perform(get(STREAM).header(TICKET_HEADER, "good"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user=" + userId)));

        verify(streamTicketPort, times(1)).consume(anyString());
    }

    @Test
    void validSellerTicket_isAuthenticatedButForbidden() throws Exception {
        givenValidTicket("seller", "ROLE_SELLER");

        mvc.perform(get(STREAM).header(TICKET_HEADER, "seller"))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownExpiredOrReusedTicket_returns401TicketInvalid() throws Exception {
        when(streamTicketPort.consume("stale")).thenReturn(Optional.empty());

        mvc.perform(get(STREAM).header(TICKET_HEADER, "stale"))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("TICKET_INVALID"));
    }

    @Test
    void secondUseOfTheSameTicket_returns401TicketInvalid() throws Exception {
        var userId = givenValidTicket("once", "ROLE_BUYER");
        when(streamTicketPort.consume("once")).thenReturn(Optional.of(userId), Optional.empty());

        mvc.perform(get(STREAM).header(TICKET_HEADER, "once"))
                .andExpect(request().asyncStarted());

        mvc.perform(get(STREAM).header(TICKET_HEADER, "once"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TICKET_INVALID"));
    }

    @Test
    void ticketOfAUserThatNoLongerExists_returns401TicketInvalid() throws Exception {
        var userId = UUID.randomUUID();
        when(streamTicketPort.consume("orphan")).thenReturn(Optional.of(userId));
        when(userDetailsAdapter.loadUserByUsername(userId.toString()))
                .thenThrow(new UsernameNotFoundException("gone"));

        mvc.perform(get(STREAM).header(TICKET_HEADER, "orphan"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TICKET_INVALID"));
    }

    @Test
    void ticketOnAnyOtherPath_isIgnoredAndNotRedeemed() throws Exception {
        mvc.perform(get("/api/lives/active").header(TICKET_HEADER, "good"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(streamTicketPort, never()).consume(any());
    }

    @Test
    void ticketOnANonGetRequestToTheStreamPath_isIgnoredAndNotRedeemed() throws Exception {
        mvc.perform(post(STREAM).header(TICKET_HEADER, "good"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(streamTicketPort, never()).consume(any());
    }

    @Test
    void noTicketAndNoBearer_returns401Unauthenticated() throws Exception {
        mvc.perform(get(STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(streamTicketPort, never()).consume(any());
    }

    // --- ticket and JWT are not interchangeable ---

    @Test
    void bearerStillOpensTheStream() throws Exception {
        var userId = UUID.randomUUID();
        var claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtService.validateAndExtract("jwt")).thenReturn(claims);
        when(userDetailsAdapter.loadUserByUsername(userId.toString()))
                .thenReturn(new UserPrincipal(userId, "u@test.com", "hash",
                        List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true));

        var started = mvc.perform(get(STREAM).header("Authorization", "Bearer jwt"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(started)).andExpect(status().isOk());
        verify(streamTicketPort, never()).consume(any());
    }

    @Test
    void ticketSentAsBearer_isNotAcceptedAsAJwt() throws Exception {
        when(jwtService.validateAndExtract("a-ticket")).thenThrow(new MalformedJwtException("not a jwt"));

        mvc.perform(get(STREAM).header("Authorization", "Bearer a-ticket"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(streamTicketPort, never()).consume(any());
    }

    @Test
    void jwtSentAsTicket_isNotAcceptedAsATicket() throws Exception {
        when(streamTicketPort.consume("eyJhbGciOiJIUzI1NiJ9.e30.sig")).thenReturn(Optional.empty());

        mvc.perform(get(STREAM).header(TICKET_HEADER, "eyJhbGciOiJIUzI1NiJ9.e30.sig"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TICKET_INVALID"));

        verify(jwtService, never()).validateAndExtract(anyString());
    }

    // --- CORS preflight of the stream (cached between reconnects, no credentials) ---

    @Test
    void streamPreflight_fromAllowedPreviewOrigin_allowsTicketAndLastEventIdWithMaxAge600() throws Exception {
        mvc.perform(options(STREAM)
                        .header("Origin", "https://livecomerce-web-git-feat-x-acme.vercel.app")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-stream-ticket,last-event-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://livecomerce-web-git-feat-x-acme.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("x-stream-ticket")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("last-event-id")))
                .andExpect(header().string("Access-Control-Max-Age", "600"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void streamPreflight_fromPreviewOfAnotherTeam_isRejected() throws Exception {
        mvc.perform(options(STREAM)
                        .header("Origin", "https://livecomerce-web-git-x-evil.vercel.app")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-stream-ticket"))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherPathsPreflight_doNotAllowTheTicketHeader() throws Exception {
        mvc.perform(options("/api/lives/active")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-stream-ticket"))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherPathsPreflight_keepTheGlobalConfiguration() throws Exception {
        mvc.perform(options("/api/lives/active")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
