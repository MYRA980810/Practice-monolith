package com.livecomerce.live.api;

import com.livecomerce.live.application.LiveFeedCardAssembler;
import com.livecomerce.live.application.LiveFeedSnapshot;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.shared.UserPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveFeedStreamController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({LiveFeedEmitterRegistry.class, LiveFeedStreamControllerTest.TestConfig.class})
@TestPropertySource(properties = "live.feed.sse.max-connections-per-user=1")
class LiveFeedStreamControllerTest {

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    /** A fresh user per test: connections stay open across tests (the context is shared). */
    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(UUID.randomUUID(), "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Autowired MockMvc mvc;

    @MockitoBean LiveFeedPort          liveFeedPort;
    @MockitoBean LiveFeedCardAssembler assembler;

    @Test
    void stream_withoutLastEventId_startsAsyncEventStreamWithAntiBufferingHeadersAndSnapshot() throws Exception {
        var snapshot = new LiveFeedSnapshot(5L, List.of(), List.of());
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        when(liveFeedPort.snapshot()).thenReturn(snapshot);
        when(assembler.withViewerCounts(snapshot)).thenReturn(snapshot);

        var result = mvc.perform(get("/api/lives/feed/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(header().string("Cache-Control", "no-cache, no-transform"))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo(
                ":connected\n\nid:5\nevent:snapshot\ndata:{\"version\":5,\"cards\":[],\"counts\":[]}\n\n");
    }

    @Test
    void stream_lastEventIdAtCurrentVersion_sendsNoSnapshot() throws Exception {
        when(liveFeedPort.currentVersion()).thenReturn(5L);

        var result = mvc.perform(get("/api/lives/feed/stream").header("Last-Event-ID", "5"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo(":connected\n\n");
        verify(liveFeedPort, never()).snapshot();
    }

    @Test
    void stream_overPerUserConnectionCap_isRejectedWith429() throws Exception {
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        mvc.perform(get("/api/lives/feed/stream").header("Last-Event-ID", "5"))
                .andExpect(request().asyncStarted());

        mvc.perform(get("/api/lives/feed/stream").header("Last-Event-ID", "5"))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isTooManyRequests());
    }
}
