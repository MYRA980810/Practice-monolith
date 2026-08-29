package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.*;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveStatus;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = LiveController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(LiveControllerTest.SecurityResolverConfig.class)
class LiveControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean CreateLiveUseCase createLiveUseCase;
    @MockitoBean StartLiveUseCase startLiveUseCase;
    @MockitoBean EndLiveUseCase endLiveUseCase;
    @MockitoBean CancelLiveUseCase cancelLiveUseCase;
    @MockitoBean RecordViewerHeartbeatUseCase recordViewerHeartbeatUseCase;
    @MockitoBean LoadLivePort loadLivePort;
    @MockitoBean ViewerCountPort viewerCountPort;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID LIVE_ID   = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        setPrincipal(SELLER_ID, "ROLE_SELLER");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "seller@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Live buildLive() {
        return Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Test Live", null, null, 60);
    }

    private static Live buildLiveWithStore(UUID storeId) {
        return Live.create(SELLER_ID, storeId, LiveContext.SELLER_PROFILE, "Store Live", null, null, 60);
    }

    // --- POST /api/lives ---

    @Test
    void createLive_withValidRequest_returns201() throws Exception {
        when(createLiveUseCase.createLive(any())).thenReturn(buildLive());

        mvc.perform(post("/api/lives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "context": "SELLER_PROFILE",
                                  "title": "My Test Live"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Live"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void createLive_withMissingTitle_returns400() throws Exception {
        mvc.perform(post("/api/lives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"context": "SELLER_PROFILE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLive_withInvalidContext_returns400() throws Exception {
        mvc.perform(post("/api/lives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"context": "INVALID_CONTEXT", "title": "Test"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/lives/{id}/start ---

    @Test
    void startLive_withValidRequest_returns200() throws Exception {
        when(startLiveUseCase.startLive(any())).thenReturn(buildLive());

        mvc.perform(post("/api/lives/{id}/start", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rtcUid": "user-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live.sellerId").value(SELLER_ID.toString()));
    }

    @Test
    void startLive_returnsBroadcastCredentials() throws Exception {
        var live = buildLive();
        live.setStreamToken("agora-rtc-token");
        live.setIvsChannel("channel-arn", "rtmps://ingest.example", "key-arn", "sk_secret_value", "https://playback.example");
        when(startLiveUseCase.startLive(any())).thenReturn(live);

        mvc.perform(post("/api/lives/{id}/start", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rtcUid": "user-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamToken").value("agora-rtc-token"))
                .andExpect(jsonPath("$.ivsIngestEndpoint").value("rtmps://ingest.example"))
                .andExpect(jsonPath("$.ivsStreamKeyValue").value("sk_secret_value"));
    }

    @Test
    void startLive_withMissingRtcUid_returns400() throws Exception {
        mvc.perform(post("/api/lives/{id}/start", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/lives/{id}/end ---

    @Test
    void endLive_asSeller_returns200() throws Exception {
        when(endLiveUseCase.endLive(any())).thenReturn(buildLive());

        mvc.perform(post("/api/lives/{id}/end", LIVE_ID))
                .andExpect(status().isOk());
    }

    // --- POST /api/lives/{id}/cancel ---

    @Test
    void cancelLive_asSeller_returns200() throws Exception {
        when(cancelLiveUseCase.cancelLive(any())).thenReturn(buildLive());

        mvc.perform(post("/api/lives/{id}/cancel", LIVE_ID))
                .andExpect(status().isOk());
    }

    // --- POST /api/lives/{id}/heartbeat ---

    @Test
    void heartbeat_withValidRequest_returns200WithCount() throws Exception {
        when(recordViewerHeartbeatUseCase.recordHeartbeat(any())).thenReturn(7L);

        mvc.perform(post("/api/lives/{id}/heartbeat", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"viewerId": "viewer-abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerCount").value(7));
    }

    @Test
    void heartbeat_withMissingViewerId_returns400() throws Exception {
        mvc.perform(post("/api/lives/{id}/heartbeat", LIVE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/lives/{id} ---

    @Test
    void getLive_whenExists_returns200() throws Exception {
        when(loadLivePort.loadById(any())).thenReturn(Optional.of(buildLive()));

        mvc.perform(get("/api/lives/{id}", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(SELLER_ID.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void getLive_neverExposesBroadcastCredentials() throws Exception {
        var live = buildLive();
        live.setStreamToken("agora-rtc-token");
        live.setIvsChannel("channel-arn", "rtmps://ingest.example", "key-arn", "sk_secret_value", "https://playback.example");
        when(loadLivePort.loadById(any())).thenReturn(Optional.of(live));

        mvc.perform(get("/api/lives/{id}", LIVE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamToken").doesNotExist())
                .andExpect(jsonPath("$.ivsIngestEndpoint").doesNotExist())
                .andExpect(jsonPath("$.ivsStreamKeyValue").doesNotExist())
                .andExpect(jsonPath("$.ivsPlaybackUrl").value("https://playback.example"));
    }

    @Test
    void getLive_whenNotFound_returns404() throws Exception {
        when(loadLivePort.loadById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/lives/{id}", LIVE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/live-not-found"));
    }

    // --- GET /api/lives/active ---

    @Test
    void listActiveLives_whenEmpty_returns200EmptyPage() throws Exception {
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listActiveLives_onlyQueriesLiveStatus_notSellerOrStoreScoped() throws Exception {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), any()))
                .thenReturn(new PageImpl<>(List.of(live)));
        when(viewerCountPort.get(any())).thenReturn(0L);

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Live"));

        verify(loadLivePort, never()).loadBySellerId(any());
        verify(loadLivePort, never()).loadByStoreId(any());
    }

    @Test
    void listActiveLives_respectsPaginationDefaults() throws Exception {
        when(loadLivePort.loadByStatus(
                eq(LiveStatus.LIVE),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startedAt")))))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk());
    }

    @Test
    void listActiveLives_withNullThumbnail_doesNotThrow() throws Exception {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), any()))
                .thenReturn(new PageImpl<>(List.of(live)));
        when(viewerCountPort.get(any())).thenReturn(3L);

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$.content[0].currentViewers").value(3));
    }

    @Test
    void listActiveLives_withNoRedisEntryYet_defaultsViewerCountToZero() throws Exception {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), any()))
                .thenReturn(new PageImpl<>(List.of(live)));
        when(viewerCountPort.get(any())).thenReturn(0L);

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentViewers").value(0));
    }

    @Test
    void listActiveLives_neverExposesPlaybackOrBroadcastCredentials() throws Exception {
        var live = buildLive();
        live.start();
        live.setStreamToken("agora-rtc-token");
        live.setIvsChannel("channel-arn", "rtmps://ingest.example", "key-arn", "sk_secret_value", "https://playback.example");
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), any()))
                .thenReturn(new PageImpl<>(List.of(live)));
        when(viewerCountPort.get(any())).thenReturn(1L);

        mvc.perform(get("/api/lives/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ivsPlaybackUrl").doesNotExist())
                .andExpect(jsonPath("$.content[0].streamToken").doesNotExist())
                .andExpect(jsonPath("$.content[0].ivsStreamKeyValue").doesNotExist())
                .andExpect(jsonPath("$.content[0].agoraChannelId").doesNotExist());
    }

    // --- GET /api/lives?sellerId= ---

    @Test
    void listBySeller_withStatusFilter_returns200() throws Exception {
        when(loadLivePort.loadBySellerIdAndStatus(eq(SELLER_ID), eq(LiveStatus.SCHEDULED)))
                .thenReturn(List.of(buildLive()));

        mvc.perform(get("/api/lives")
                        .param("sellerId", SELLER_ID.toString())
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void listBySeller_withoutStatusFilter_returns200() throws Exception {
        when(loadLivePort.loadBySellerId(SELLER_ID)).thenReturn(List.of(buildLive()));

        mvc.perform(get("/api/lives").param("sellerId", SELLER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- GET /api/stores/{storeId}/lives ---

    @Test
    void listByStore_withScheduledStatus_returns200WithList() throws Exception {
        when(loadLivePort.loadByStoreIdAndStatus(eq(STORE_ID), eq(LiveStatus.SCHEDULED)))
                .thenReturn(List.of(buildLiveWithStore(STORE_ID)));

        mvc.perform(get("/api/stores/{storeId}/lives", STORE_ID)
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void listByStore_withLiveStatus_returns200() throws Exception {
        mvc.perform(get("/api/stores/{storeId}/lives", STORE_ID)
                        .param("status", "LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listByStore_unknownStore_returns200EmptyList() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(loadLivePort.loadByStoreId(eq(unknownId))).thenReturn(List.of());

        mvc.perform(get("/api/stores/{storeId}/lives", unknownId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listByStore_withoutStatusFilter_returns200WithAllLives() throws Exception {
        when(loadLivePort.loadByStoreId(eq(STORE_ID)))
                .thenReturn(List.of(buildLiveWithStore(STORE_ID)));

        mvc.perform(get("/api/stores/{storeId}/lives", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].storeId").value(STORE_ID.toString()));
    }

    @Test
    void listByStore_withEndedStatus_returnsOnlyEndedLives() throws Exception {
        when(loadLivePort.loadByStoreIdAndStatus(eq(STORE_ID), eq(LiveStatus.ENDED)))
                .thenReturn(List.of());

        mvc.perform(get("/api/stores/{storeId}/lives", STORE_ID)
                        .param("status", "ENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listByStore_storeWithNoLives_returns200EmptyArray() throws Exception {
        when(loadLivePort.loadByStoreId(eq(STORE_ID))).thenReturn(List.of());

        mvc.perform(get("/api/stores/{storeId}/lives", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
