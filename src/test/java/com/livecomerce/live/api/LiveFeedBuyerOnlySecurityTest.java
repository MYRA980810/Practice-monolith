package com.livecomerce.live.api;

import com.livecomerce.auth.infrastructure.security.ProductionSecurityTestConfig;
import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.LoadStoreNamesPort;
import com.livecomerce.live.application.LiveFeedCardAssembler;
import com.livecomerce.live.application.port.in.*;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.shared.UserPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The lives feed (listing, chips-bar counts, SSE stream and RTM feed token) is BUYER-only.
 * Runs against the production security filter chain and method security: BUYER gets 2xx,
 * any other role gets 403 with a ProblemDetail body, no credentials gets 401.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = {LiveController.class, LiveFeedStreamController.class, LiveFeedTokenController.class,
                LiveFeedTicketController.class},
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({ProductionSecurityTestConfig.class, LiveFeedCardAssembler.class, LiveFeedEmitterRegistry.class,
        LiveFeedBuyerOnlySecurityTest.MetricsConfig.class})
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "live.feed.sse.max-connections-per-user=100"
})
class LiveFeedBuyerOnlySecurityTest {

    @TestConfiguration
    static class MetricsConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private static final String STREAM = "/api/lives/feed/stream";

    @Autowired MockMvc mvc;

    @MockitoBean CreateLiveUseCase createLiveUseCase;
    @MockitoBean StartLiveUseCase startLiveUseCase;
    @MockitoBean EndLiveUseCase endLiveUseCase;
    @MockitoBean CancelLiveUseCase cancelLiveUseCase;
    @MockitoBean ChangeLiveCategoryUseCase changeLiveCategoryUseCase;
    @MockitoBean RecordViewerHeartbeatUseCase recordViewerHeartbeatUseCase;
    @MockitoBean GetLiveFeedTokenUseCase getLiveFeedTokenUseCase;
    @MockitoBean IssueFeedStreamTicketUseCase issueFeedStreamTicketUseCase;
    @MockitoBean LoadLivePort loadLivePort;
    @MockitoBean LiveFeedPort liveFeedPort;
    @MockitoBean ViewerCountPort viewerCountPort;
    @MockitoBean LoadStoreNamesPort loadStoreNamesPort;
    @MockitoBean LoadSellerNamesPort loadSellerNamesPort;

    @BeforeEach
    void stubFeedQueries() {
        when(loadLivePort.loadByStatus(any(), any())).thenReturn(Page.empty());
        when(loadLivePort.loadUpcoming(any())).thenReturn(Page.empty());
        when(liveFeedPort.currentVersion()).thenReturn(7L);
        when(getLiveFeedTokenUseCase.getFeedToken(any()))
                .thenReturn(new GetLiveFeedTokenUseCase.FeedTokenResult("token", "lives-feed", "app-id"));
    }

    private static RequestPostProcessor as(String... authorities) {
        var principal = new UserPrincipal(UUID.randomUUID(), "user@test.com", "hash",
                Arrays.stream(authorities).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList(), true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // --- REST feed endpoints ---

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/lives/active",
            "/api/lives/upcoming",
            "/api/lives/active/category-counts",
            "/api/lives/upcoming/category-counts",
            "/api/lives/rtm/feed-token"
    })
    void feedEndpoint_asBuyer_returns200(String path) throws Exception {
        mvc.perform(get(path).with(as("ROLE_BUYER")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/lives/active",
            "/api/lives/upcoming",
            "/api/lives/active/category-counts",
            "/api/lives/upcoming/category-counts",
            "/api/lives/rtm/feed-token"
    })
    void feedEndpoint_asSeller_returns403ProblemDetail(String path) throws Exception {
        mvc.perform(get(path).with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/lives/active",
            "/api/lives/upcoming",
            "/api/lives/active/category-counts",
            "/api/lives/upcoming/category-counts",
            "/api/lives/rtm/feed-token"
    })
    void feedEndpoint_withoutRole_returns403(String path) throws Exception {
        mvc.perform(get(path).with(as()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/lives/active",
            "/api/lives/upcoming",
            "/api/lives/active/category-counts",
            "/api/lives/upcoming/category-counts",
            "/api/lives/rtm/feed-token"
    })
    void feedEndpoint_unauthenticated_returns401(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void sellerIsDeniedBeforeAnyFeedQueryRuns() throws Exception {
        mvc.perform(get("/api/lives/active").with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden());

        verify(loadLivePort, never()).loadByStatus(any(), any());
        verifyNoInteractions(liveFeedPort);
    }

    // --- SSE stream ---

    @Test
    void stream_asBuyer_startsEventStream() throws Exception {
        mvc.perform(get(STREAM).accept(MediaType.TEXT_EVENT_STREAM)
                        .header("Last-Event-ID", "7")
                        .with(as("ROLE_BUYER")))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    /**
     * The SSE client sends {@code Accept: text/event-stream}; the 403 must still be rendered as a
     * ProblemDetail (not fail with 406 / HttpMediaTypeNotAcceptableException) and must be raised
     * before an emitter is registered or async processing starts.
     */
    @Test
    void stream_asSeller_isRejectedWith403BeforeTheStreamStarts() throws Exception {
        mvc.perform(get(STREAM).accept(MediaType.TEXT_EVENT_STREAM)
                        .header("Last-Event-ID", "7")
                        .with(as("ROLE_SELLER")))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(liveFeedPort);
    }

    @Test
    void stream_unauthenticated_isRejectedWith401() throws Exception {
        mvc.perform(get(STREAM).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(liveFeedPort);
    }

    // --- SSE stream ticket (issued to the Next.js server, which holds the Bearer token) ---

    private static final String STREAM_TICKET = "/api/lives/feed/stream-ticket";

    @Test
    void streamTicket_asBuyer_returnsTicketAndExpiry_notCacheable() throws Exception {
        when(issueFeedStreamTicketUseCase.issue(any()))
                .thenReturn(new IssueFeedStreamTicketUseCase.FeedStreamTicket("opaque-ticket", 30_000L));

        mvc.perform(post(STREAM_TICKET).with(as("ROLE_BUYER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.ticket").value("opaque-ticket"))
                .andExpect(jsonPath("$.expiresInMs").value(30_000));
    }

    @Test
    void streamTicket_isIssuedForTheAuthenticatedUser() throws Exception {
        var userId = UUID.randomUUID();
        var principal = new UserPrincipal(userId, "buyer@test.com", "hash",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true);
        when(issueFeedStreamTicketUseCase.issue(userId))
                .thenReturn(new IssueFeedStreamTicketUseCase.FeedStreamTicket("t", 30_000L));

        mvc.perform(post(STREAM_TICKET).with(authentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk());

        verify(issueFeedStreamTicketUseCase).issue(userId);
    }

    @Test
    void streamTicket_asSeller_returns403WithoutIssuing() throws Exception {
        mvc.perform(post(STREAM_TICKET).with(as("ROLE_SELLER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(issueFeedStreamTicketUseCase);
    }

    @Test
    void streamTicket_unauthenticated_returns401() throws Exception {
        mvc.perform(post(STREAM_TICKET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(issueFeedStreamTicketUseCase);
    }
}
