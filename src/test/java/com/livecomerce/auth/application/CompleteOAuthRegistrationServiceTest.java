package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.CompleteOAuthRegistrationUseCase.CompleteOAuthCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteOAuthRegistrationServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;

    @InjectMocks CompleteOAuthRegistrationService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OAUTH_PENDING_TOKEN = "oauth.pending.jwt";

    @Test
    void complete_withValidToken_assignsRoleAndReturnsFullJwt() {
        var user = User.createFromOAuth("u@test.com", "John", "Doe", "google", "sub123", null);

        when(tokenGeneratorPort.extractUserIdFromOAuthPendingToken(OAUTH_PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(saveUserPort.save(any())).thenReturn(user);
        when(tokenGeneratorPort.generate(any())).thenReturn("full-jwt");

        var result = service.complete(new CompleteOAuthCommand(OAUTH_PENDING_TOKEN, Role.SELLER));

        assertThat(result.accessToken()).isEqualTo("full-jwt");
        assertThat(user.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    void complete_withValidToken_assignsBuyerRole() {
        var user = User.createFromOAuth("buyer@test.com", "Jane", "Doe", "google", "sub456", null);

        when(tokenGeneratorPort.extractUserIdFromOAuthPendingToken(OAUTH_PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(saveUserPort.save(any())).thenReturn(user);
        when(tokenGeneratorPort.generate(any())).thenReturn("full-jwt-buyer");

        service.complete(new CompleteOAuthCommand(OAUTH_PENDING_TOKEN, Role.BUYER));

        assertThat(user.getRole()).isEqualTo(Role.BUYER);
    }

    @Test
    void complete_withInvalidToken_throwsOAuthPendingTokenInvalid() {
        when(tokenGeneratorPort.extractUserIdFromOAuthPendingToken(OAUTH_PENDING_TOKEN))
                .thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> service.complete(new CompleteOAuthCommand(OAUTH_PENDING_TOKEN, Role.BUYER)))
                .isInstanceOf(OAuthPendingTokenInvalidException.class);
    }
}
