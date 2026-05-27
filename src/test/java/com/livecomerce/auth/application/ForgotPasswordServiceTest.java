package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ForgotPasswordUseCase.ForgotPasswordCommand;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks ForgotPasswordService service;

    @Test
    void forgotPassword_withEmail_sendsOtpAndReturnsPendingToken() {
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.BUYER);
        when(loadUserPort.loadByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generateResetPendingToken(user)).thenReturn("reset-pending-jwt");

        var result = service.forgotPassword(new ForgotPasswordCommand("user@test.com"));

        assertThat(result.pendingToken()).isNotBlank();
        assertThat(result.channel()).isEqualTo(VerificationChannel.EMAIL);
    }

    @Test
    void forgotPassword_withPhone_sendsOtpAndReturnsSmsChannel() {
        var user = User.create(null, "hash", "John", "Doe", "+5491112345678", Role.BUYER);
        when(loadUserPort.loadByPhone("+5491112345678")).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generateResetPendingToken(user)).thenReturn("reset-pending-jwt");

        var result = service.forgotPassword(new ForgotPasswordCommand("+5491112345678"));

        assertThat(result.pendingToken()).isNotBlank();
        assertThat(result.channel()).isEqualTo(VerificationChannel.SMS);
    }

    @Test
    void forgotPassword_withUnknownContact_throwsContactNotFoundException() {
        when(loadUserPort.loadByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordCommand("unknown@test.com")))
                .isInstanceOf(ContactNotFoundException.class);
    }
}
