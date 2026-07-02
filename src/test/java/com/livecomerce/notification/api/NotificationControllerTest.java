package com.livecomerce.notification.api;

import com.livecomerce.notification.application.port.in.MarkNotificationReadUseCase;
import com.livecomerce.notification.application.port.out.LoadNotificationPort;
import com.livecomerce.notification.domain.Notification;
import com.livecomerce.notification.domain.NotificationNotOwnedException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(NotificationControllerTest.SecurityResolverConfig.class)
class NotificationControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean LoadNotificationPort         loadNotificationPort;
    @MockitoBean MarkNotificationReadUseCase  markNotificationReadUseCase;

    private static final UUID BUYER_ID       = UUID.randomUUID();
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUpBuyerPrincipal() {
        setPrincipal(BUYER_ID, "ROLE_BUYER");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(UUID userId, String role) {
        var principal = new UserPrincipal(
                userId, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority(role)), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Notification buildNotification(UUID userId) {
        return Notification.create(userId, "live-started", UUID.randomUUID(),
                Map.of("type", "live-started"));
    }

    // --- GET /api/notifications ---

    @Test
    void getNotifications_asBuyer_returns200WithPaginatedList() throws Exception {
        var notification = buildNotification(BUYER_ID);
        var page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);
        when(loadNotificationPort.loadByUserId(eq(BUYER_ID), any())).thenReturn(page);

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("live-started"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getNotifications_unauthenticated_returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/notifications/{id}/read ---

    @Test
    void markRead_asOwner_returns200() throws Exception {
        var notification = buildNotification(BUYER_ID);
        when(loadNotificationPort.loadById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        mvc.perform(post("/api/notifications/{id}/read", NOTIFICATION_ID))
                .andExpect(status().isOk());
    }

    @Test
    void markRead_forAnotherUsersNotification_returns403() throws Exception {
        doThrow(new NotificationNotOwnedException(NOTIFICATION_ID))
                .when(markNotificationReadUseCase).markRead(any());

        mvc.perform(post("/api/notifications/{id}/read", NOTIFICATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("https://livecomerce.com/errors/notification-not-owned"));
    }
}
