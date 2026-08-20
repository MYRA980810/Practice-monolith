package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.VerifyChangePasswordOtpUseCase.VerifyChangePasswordOtpCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyChangePasswordOtpServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks VerifyChangePasswordOtpService service;

    @Test
    void verify_withValidCode_returnsChangePasswordToken() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromChangePasswordPendingToken("valid-pending")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check(user.resolveRecipient(), "123456", user.resolveChannel())).thenReturn(true);
        when(tokenGeneratorPort.generateChangePasswordToken(user)).thenReturn("change-password-jwt");

        var result = service.verify(new VerifyChangePasswordOtpCommand(userId, "valid-pending", "123456"));

        assertThat(result.changePasswordToken()).isEqualTo("change-password-jwt");
    }

    @Test
    void verify_withInvalidOtp_throwsInvalidOtpException() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromChangePasswordPendingToken("valid-pending")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check(user.resolveRecipient(), "wrong", user.resolveChannel())).thenReturn(false);

        assertThatThrownBy(() -> service.verify(new VerifyChangePasswordOtpCommand(userId, "valid-pending", "wrong")))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void verify_withInvalidPendingToken_throwsChangePasswordPendingTokenInvalidException() {
        var userId = UUID.randomUUID();
        when(tokenGeneratorPort.extractUserIdFromChangePasswordPendingToken("bad-token"))
                .thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> service.verify(new VerifyChangePasswordOtpCommand(userId, "bad-token", "123456")))
                .isInstanceOf(ChangePasswordPendingTokenInvalidException.class);
    }

    @Test
    void verify_withPendingTokenBelongingToDifferentUser_throwsChangePasswordPendingTokenInvalidException() {
        var authenticatedUserId = UUID.randomUUID();
        var tokenOwnerUserId = UUID.randomUUID();
        when(tokenGeneratorPort.extractUserIdFromChangePasswordPendingToken("someone-elses-pending"))
                .thenReturn(tokenOwnerUserId);

        assertThatThrownBy(() -> service.verify(
                new VerifyChangePasswordOtpCommand(authenticatedUserId, "someone-elses-pending", "123456")))
                .isInstanceOf(ChangePasswordPendingTokenInvalidException.class);
    }
}
