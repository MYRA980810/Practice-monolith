package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.VerifyCurrentPasswordUseCase.VerifyCurrentPasswordCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.VerificationChannel;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyCurrentPasswordServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks VerifyCurrentPasswordService service;

    @Test
    void verifyCurrentPassword_withCorrectPassword_sendsOtpAndReturnsPendingToken() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "current-hash", "John", "Doe", null, Role.BUYER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass123", "current-hash")).thenReturn(true);
        when(tokenGeneratorPort.generateChangePasswordPendingToken(user)).thenReturn("change-password-pending-jwt");

        var result = service.verifyCurrentPassword(new VerifyCurrentPasswordCommand(userId, "currentPass123"));

        assertThat(result.pendingToken()).isEqualTo("change-password-pending-jwt");
        assertThat(result.channel()).isEqualTo(VerificationChannel.EMAIL);
        verify(verifyOtpPort).send(user.resolveRecipient(), VerificationChannel.EMAIL);
    }

    @Test
    void verifyCurrentPassword_withIncorrectPassword_throwsCurrentPasswordInvalidException() {
        var userId = UUID.randomUUID();
        var user = User.create("user@test.com", "current-hash", "John", "Doe", null, Role.BUYER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "current-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyCurrentPassword(new VerifyCurrentPasswordCommand(userId, "wrongPass")))
                .isInstanceOf(CurrentPasswordInvalidException.class);
    }
}
