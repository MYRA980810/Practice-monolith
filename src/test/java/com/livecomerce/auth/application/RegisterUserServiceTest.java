package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.RegisterUserUseCase.RegisterCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.VerificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks RegisterUserService service;

    private static final RegisterCommand EMAIL_COMMAND = new RegisterCommand(
            "buyer@test.com", "secret123", "Jane", "Doe", Role.BUYER
    );

    private static final RegisterCommand PHONE_COMMAND = new RegisterCommand(
            "+5491112345678", "secret123", "Jane", "Doe", Role.BUYER
    );

    @Test
    void register_withEmailContact_returnsPendingTokenWithEmailChannel() {
        var saved = User.create("buyer@test.com", "bcrypt", "Jane", "Doe", null, Role.BUYER);
        when(loadUserPort.loadByEmail("buyer@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(saved)).thenReturn("pending-jwt");

        var result = service.register(EMAIL_COMMAND);

        assertThat(result.pendingToken()).isEqualTo("pending-jwt");
        assertThat(result.channel()).isEqualTo(VerificationChannel.EMAIL);
    }

    @Test
    void register_withPhoneContact_returnsPendingTokenWithSmsChannel() {
        var saved = User.create(null, "bcrypt", "Jane", "Doe", "+5491112345678", Role.BUYER);
        when(loadUserPort.loadByPhone("+5491112345678")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(saved)).thenReturn("pending-jwt");

        var result = service.register(PHONE_COMMAND);

        assertThat(result.pendingToken()).isEqualTo("pending-jwt");
        assertThat(result.channel()).isEqualTo(VerificationChannel.SMS);
    }

    @Test
    void register_withExistingEmailContact_throwsEmailAlreadyTaken() {
        var existing = User.create("buyer@test.com", "hash", "X", "Y", null, Role.BUYER);
        when(loadUserPort.loadByEmail("buyer@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(EMAIL_COMMAND))
                .isInstanceOf(EmailAlreadyTakenException.class);

        verify(saveUserPort, never()).save(any());
        verifyNoInteractions(verifyOtpPort);
    }

    @Test
    void register_withExistingPhoneContact_throwsPhoneAlreadyTaken() {
        var existing = User.create(null, "hash", "X", "Y", "+5491112345678", Role.BUYER);
        when(loadUserPort.loadByPhone("+5491112345678")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(PHONE_COMMAND))
                .isInstanceOf(PhoneAlreadyTakenException.class);

        verify(saveUserPort, never()).save(any());
        verifyNoInteractions(verifyOtpPort);
    }

    @Test
    void register_withEmailContact_sendsOtpToEmail() {
        var saved = User.create("buyer@test.com", "bcrypt", "Jane", "Doe", null, Role.BUYER);
        when(loadUserPort.loadByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(any())).thenReturn("pending");

        service.register(EMAIL_COMMAND);

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var channelCaptor = ArgumentCaptor.forClass(VerificationChannel.class);
        verify(verifyOtpPort).send(toCaptor.capture(), channelCaptor.capture());
        assertThat(toCaptor.getValue()).isEqualTo("buyer@test.com");
        assertThat(channelCaptor.getValue()).isEqualTo(VerificationChannel.EMAIL);
    }

    @Test
    void register_withPhoneContact_sendsOtpToPhone() {
        var saved = User.create(null, "bcrypt", "Jane", "Doe", "+5491112345678", Role.BUYER);
        when(loadUserPort.loadByPhone(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(any())).thenReturn("pending");

        service.register(PHONE_COMMAND);

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var channelCaptor = ArgumentCaptor.forClass(VerificationChannel.class);
        verify(verifyOtpPort).send(toCaptor.capture(), channelCaptor.capture());
        assertThat(toCaptor.getValue()).isEqualTo("+5491112345678");
        assertThat(channelCaptor.getValue()).isEqualTo(VerificationChannel.SMS);
    }

    @Test
    void register_encodesPasswordBeforeSaving() {
        var saved = User.create("buyer@test.com", "bcrypt-hash", "Jane", "Doe", null, Role.BUYER);
        when(loadUserPort.loadByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(any())).thenReturn("pending");

        service.register(EMAIL_COMMAND);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void register_doesNotIssueFullJwt() {
        var saved = User.create("buyer@test.com", "bcrypt", "Jane", "Doe", null, Role.BUYER);
        when(loadUserPort.loadByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generatePendingToken(any())).thenReturn("pending");

        service.register(EMAIL_COMMAND);

        verify(tokenGeneratorPort, never()).generate(any());
    }
}
