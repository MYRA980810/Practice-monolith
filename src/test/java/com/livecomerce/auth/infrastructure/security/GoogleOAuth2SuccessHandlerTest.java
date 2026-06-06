package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import com.livecomerce.auth.application.port.out.OAuthTokenType;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2SuccessHandlerTest {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock OAuthCodeStorePort codeStorePort;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock Authentication authentication;
    @Mock OAuth2User oAuth2User;

    GoogleOAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GoogleOAuth2SuccessHandler(
                loadUserPort, saveUserPort, codeStorePort,
                "http://localhost:3000", 90L
        );
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-sub-123");
        when(oAuth2User.getAttribute("email")).thenReturn("user@test.com");
        when(oAuth2User.getAttribute("given_name")).thenReturn("John");
        when(oAuth2User.getAttribute("family_name")).thenReturn("Doe");
        when(oAuth2User.getAttribute("picture")).thenReturn(null);
    }

    @Test
    void existingUser_storesFullPayloadAndRedirectContainsCode() throws IOException {
        var user = User.createFromOAuth("user@test.com", "John", "Doe", "google", "sub-123", null);
        user.assignRole(Role.SELLER);
        when(loadUserPort.loadByProvider("google", "google-sub-123")).thenReturn(Optional.of(user));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OAuthCodePayload> payloadCaptor = ArgumentCaptor.forClass(OAuthCodePayload.class);
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(codeStorePort).store(codeCaptor.capture(), payloadCaptor.capture(), eq(Duration.ofSeconds(90L)));
        verify(response).sendRedirect(redirectCaptor.capture());

        // Code must be UUID format
        assertThat(UUID_PATTERN.matcher(codeCaptor.getValue()).matches()).isTrue();

        // Payload must be FULL type
        assertThat(payloadCaptor.getValue().tokenType()).isEqualTo(OAuthTokenType.FULL);

        // Redirect must contain ?code= and NOT token=
        String redirect = redirectCaptor.getValue();
        assertThat(redirect).contains("?code=");
        assertThat(redirect).doesNotContain("token=");
        assertThat(redirect).contains("/auth/oauth-callback");

        // Code in redirect must match the stored code
        assertThat(redirect).contains(codeCaptor.getValue());
    }

    @Test
    void newUser_storesOauthPendingPayloadAndRedirectsToSelectRole() throws IOException {
        when(loadUserPort.loadByProvider("google", "google-sub-123")).thenReturn(Optional.empty());
        when(loadUserPort.loadByEmail("user@test.com")).thenReturn(Optional.empty());
        var savedUser = User.createFromOAuth("user@test.com", "John", "Doe", "google", "google-sub-123", null);
        when(saveUserPort.save(any())).thenReturn(savedUser);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OAuthCodePayload> payloadCaptor = ArgumentCaptor.forClass(OAuthCodePayload.class);
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(codeStorePort).store(codeCaptor.capture(), payloadCaptor.capture(), eq(Duration.ofSeconds(90L)));
        verify(response).sendRedirect(redirectCaptor.capture());

        // Payload must be OAUTH_PENDING
        assertThat(payloadCaptor.getValue().tokenType()).isEqualTo(OAuthTokenType.OAUTH_PENDING);

        // Redirect must go to select-role with code
        String redirect = redirectCaptor.getValue();
        assertThat(redirect).contains("/auth/select-role");
        assertThat(redirect).contains("?code=");
        assertThat(redirect).doesNotContain("token=");
    }

    @Test
    void emailConflict_doesNotCallCodeStore_redirectsWithErrorParam() throws IOException {
        when(loadUserPort.loadByProvider("google", "google-sub-123")).thenReturn(Optional.empty());
        var conflictUser = User.create("user@test.com", "hash", "Jane", "Doe", null, Role.SELLER);
        when(loadUserPort.loadByEmail("user@test.com")).thenReturn(Optional.of(conflictUser));

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(codeStorePort, never()).store(anyString(), any(), any());
        verify(response).sendRedirect(redirectCaptor.capture());
        assertThat(redirectCaptor.getValue()).contains("error=email_conflict");
    }

    @Test
    void existingUser_storedCodeIsUuidFormat() throws IOException {
        var user = User.createFromOAuth("user@test.com", "John", "Doe", "google", "sub-123", null);
        user.assignRole(Role.SELLER);
        when(loadUserPort.loadByProvider("google", "google-sub-123")).thenReturn(Optional.of(user));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(codeStorePort).store(codeCaptor.capture(), any(), any());
        assertThat(codeCaptor.getValue()).hasSize(36);
        assertThat(UUID_PATTERN.matcher(codeCaptor.getValue()).matches()).isTrue();
    }
}
