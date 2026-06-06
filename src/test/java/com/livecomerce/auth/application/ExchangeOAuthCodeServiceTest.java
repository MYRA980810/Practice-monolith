package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.ExchangeOAuthCodeUseCase.ExchangeCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import com.livecomerce.auth.application.port.out.OAuthTokenType;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeOAuthCodeServiceTest {

    @Mock OAuthCodeStorePort codeStorePort;
    @Mock LoadUserPort loadUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;

    @InjectMocks ExchangeOAuthCodeService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CODE = "test-exchange-code";

    private User existingUser() {
        var user = User.createFromOAuth("oauth@test.com", "John", "Doe", "google", "sub-123", null);
        user.assignRole(Role.SELLER);
        return user;
    }

    @Test
    void exchange_withFullCode_callsGenerateAndReturnsFullAuthResult() {
        var user = existingUser();
        var payload = new OAuthCodePayload(USER_ID, OAuthTokenType.FULL);
        when(codeStorePort.exchange(CODE)).thenReturn(Optional.of(payload));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generate(user)).thenReturn("full-jwt");

        AuthResult result = service.exchange(new ExchangeCommand(CODE));

        assertThat(result.accessToken()).isEqualTo("full-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        verify(codeStorePort).exchange(CODE);
        verify(tokenGeneratorPort).generate(user);
        verifyNoMoreInteractions(tokenGeneratorPort);
    }

    @Test
    void exchange_withOauthPendingCode_callsGenerateOAuthPendingAndReturnsResult() {
        var user = existingUser();
        var payload = new OAuthCodePayload(USER_ID, OAuthTokenType.OAUTH_PENDING);
        when(codeStorePort.exchange(CODE)).thenReturn(Optional.of(payload));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generateOAuthPendingToken(user)).thenReturn("pending-jwt");

        AuthResult result = service.exchange(new ExchangeCommand(CODE));

        assertThat(result.accessToken()).isEqualTo("pending-jwt");
        verify(codeStorePort).exchange(CODE);
        verify(tokenGeneratorPort).generateOAuthPendingToken(user);
        verifyNoMoreInteractions(tokenGeneratorPort);
    }

    @Test
    void exchange_whenCodeNotFound_throwsOAuthCodeInvalidException() {
        when(codeStorePort.exchange(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange(new ExchangeCommand(CODE)))
                .isInstanceOf(OAuthCodeInvalidException.class);

        verify(codeStorePort).exchange(CODE);
    }

    @Test
    void exchange_whenUserNotFoundAfterValidCode_throwsIllegalStateException() {
        var payload = new OAuthCodePayload(USER_ID, OAuthTokenType.FULL);
        when(codeStorePort.exchange(CODE)).thenReturn(Optional.of(payload));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange(new ExchangeCommand(CODE)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exchange_callsPortExchangeExactlyOnce() {
        var payload = new OAuthCodePayload(USER_ID, OAuthTokenType.FULL);
        var user = existingUser();
        when(codeStorePort.exchange(CODE)).thenReturn(Optional.of(payload));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generate(user)).thenReturn("jwt");

        service.exchange(new ExchangeCommand(CODE));

        verify(codeStorePort).exchange(CODE);
        verifyNoMoreInteractions(codeStorePort);
    }
}
