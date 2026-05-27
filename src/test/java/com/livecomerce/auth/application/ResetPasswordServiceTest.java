package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ResetPasswordUseCase.ResetPasswordCommand;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks ResetPasswordService service;

    @Test
    void reset_withMatchingPasswords_updatesHashAndReturnsAuthResult() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "old-hash", "John", "Doe", null, Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromPasswordResetToken("valid-reset")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-encoded-hash");
        when(saveUserPort.save(any())).thenReturn(user);
        when(tokenGeneratorPort.generate(any())).thenReturn("access-jwt");

        var result = service.reset(new ResetPasswordCommand("valid-reset", "newPass123", "newPass123"));

        assertThat(result.accessToken()).isNotBlank();
    }

    @Test
    void reset_withMismatchedPasswords_throwsPasswordMismatchException() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "old-hash", "John", "Doe", null, Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromPasswordResetToken("valid-reset")).thenReturn(userId);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reset(
                new ResetPasswordCommand("valid-reset", "newPass123", "differentPass")))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    void reset_withInvalidResetToken_throwsPasswordResetTokenInvalidException() {
        when(tokenGeneratorPort.extractUserIdFromPasswordResetToken("bad-token"))
                .thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> service.reset(
                new ResetPasswordCommand("bad-token", "newPass123", "newPass123")))
                .isInstanceOf(PasswordResetTokenInvalidException.class);
    }
}
