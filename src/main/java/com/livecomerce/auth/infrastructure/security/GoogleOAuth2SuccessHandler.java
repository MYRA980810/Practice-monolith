package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import com.livecomerce.auth.application.port.out.OAuthTokenType;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final OAuthCodeStorePort codeStorePort;
    private final String frontendUrl;
    private final long codeTtlSeconds;

    GoogleOAuth2SuccessHandler(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            OAuthCodeStorePort codeStorePort,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.oauth2.code-ttl-seconds}") long codeTtlSeconds) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.codeStorePort = codeStorePort;
        this.frontendUrl = frontendUrl;
        this.codeTtlSeconds = codeTtlSeconds;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerId = oAuth2User.getAttribute("sub");
        String email      = oAuth2User.getAttribute("email");
        String firstName  = oAuth2User.getAttribute("given_name");
        String lastName   = oAuth2User.getAttribute("family_name");
        String avatarUrl  = oAuth2User.getAttribute("picture");

        // 1. Existing user by provider → store FULL code → redirect to callback
        var existingByProvider = loadUserPort.loadByProvider("google", providerId);
        if (existingByProvider.isPresent()) {
            var user = existingByProvider.get();
            var code = UUID.randomUUID().toString();
            codeStorePort.store(code, new OAuthCodePayload(user.getId(), OAuthTokenType.FULL),
                    Duration.ofSeconds(codeTtlSeconds));
            log.info("Existing Google user logged in: {}", email);
            response.sendRedirect(frontendUrl + "/auth/oauth-callback?code=" + code);
            return;
        }

        // 2. Email conflict — registered with a different method
        var existingByEmail = loadUserPort.loadByEmail(email);
        if (existingByEmail.isPresent()) {
            log.warn("OAuth email conflict for: {}", email);
            response.sendRedirect(frontendUrl + "/auth/login?error=email_conflict");
            return;
        }

        // 3. New user — create, issue oauth-pending code, redirect to role selection
        var user  = User.createFromOAuth(email, firstName, lastName, "google", providerId, avatarUrl);
        var saved = saveUserPort.save(user);
        var code  = UUID.randomUUID().toString();
        codeStorePort.store(code, new OAuthCodePayload(saved.getId(), OAuthTokenType.OAUTH_PENDING),
                Duration.ofSeconds(codeTtlSeconds));
        log.info("New Google user registered, awaiting role selection: {}", email);
        response.sendRedirect(frontendUrl + "/auth/select-role?code=" + code);
    }
}
