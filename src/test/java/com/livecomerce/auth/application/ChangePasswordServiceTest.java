package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ChangePasswordUseCase.ChangePasswordCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenStorePort refreshTokenStorePort;
    @Mock RefreshTokenHasher refreshTokenHasher;

    @InjectMocks ChangePasswordService service;

    @Test
    void changePassword_withValidTokenAndCurrentRefreshToken_updatesHashAndRevokesOtherFamilies() {
        var userId = UUID.randomUUID();
        var familyId = UUID.randomUUID();
        var user = User.create("user@test.com", "old-hash", "John", "Doe", null, Role.BUYER);
        var currentToken = RefreshToken.issue(userId, familyId, "hashed-current", 1000L);

        when(tokenGeneratorPort.extractUserIdFromChangePasswordToken("valid-change")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-encoded-hash");
        when(saveUserPort.save(any())).thenReturn(user);
        when(refreshTokenHasher.hash("raw-current-refresh")).thenReturn("hashed-current");
        when(refreshTokenStorePort.loadByTokenHash("hashed-current")).thenReturn(Optional.of(currentToken));

        service.changePassword(new ChangePasswordCommand(
                userId, "valid-change", "newPass123", "newPass123", "raw-current-refresh"));

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-hash");
        verify(saveUserPort).save(user);
        verify(refreshTokenStorePort).revokeAllForUserExcept(userId, familyId);
    }

    @Test
    void changePassword_withBlankCurrentRefreshToken_revokesAllFamiliesForUser() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "old-hash", "John", "Doe", null, Role.BUYER);

        when(tokenGeneratorPort.extractUserIdFromChangePasswordToken("valid-change")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-encoded-hash");
        when(saveUserPort.save(any())).thenReturn(user);

        service.changePassword(new ChangePasswordCommand(
                userId, "valid-change", "newPass123", "newPass123", null));

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-hash");
        verify(refreshTokenStorePort).revokeAllForUserExcept(eq(userId), isNull());
    }

    @Test
    void changePassword_withMismatchedPasswords_throwsPasswordMismatchException() {
        var userId = UUID.randomUUID();
        when(tokenGeneratorPort.extractUserIdFromChangePasswordToken("valid-change")).thenReturn(userId);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(
                userId, "valid-change", "newPass123", "differentPass", null)))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    void changePassword_withInvalidChangePasswordToken_throwsChangePasswordTokenInvalidException() {
        var userId = UUID.randomUUID();
        when(tokenGeneratorPort.extractUserIdFromChangePasswordToken("bad-token"))
                .thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(
                userId, "bad-token", "newPass123", "newPass123", null)))
                .isInstanceOf(ChangePasswordTokenInvalidException.class);
    }

    @Test
    void changePassword_withTokenBelongingToDifferentUser_throwsChangePasswordTokenInvalidException() {
        var authenticatedUserId = UUID.randomUUID();
        var tokenOwnerUserId = UUID.randomUUID();
        when(tokenGeneratorPort.extractUserIdFromChangePasswordToken("someone-elses-token"))
                .thenReturn(tokenOwnerUserId);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(
                authenticatedUserId, "someone-elses-token", "newPass123", "newPass123", null)))
                .isInstanceOf(ChangePasswordTokenInvalidException.class);
    }
}
