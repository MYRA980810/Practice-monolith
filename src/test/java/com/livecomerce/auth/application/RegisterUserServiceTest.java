package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.RegisterUserUseCase.RegisterCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.UserRegisteredEvent;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RegisterUserService service;

    private static final RegisterCommand VALID_COMMAND = new RegisterCommand(
            "buyer@test.com", "secret123", "Jane", "Doe", "+5491111", Role.BUYER
    );

    @Test
    void register_withNewEmail_savesUserAndReturnsToken() {
        var saved = User.create("buyer@test.com", "bcrypt", "Jane", "Doe", "+5491111", Role.BUYER);
        when(loadUserPort.loadByEmail("buyer@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generate(saved)).thenReturn("jwt-token");

        var result = service.register(VALID_COMMAND);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("buyer@test.com");
        assertThat(result.role()).isEqualTo(Role.BUYER);
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void register_withExistingEmail_throwsEmailAlreadyTaken() {
        var existing = User.create("buyer@test.com", "hash", "X", "Y", null, Role.BUYER);
        when(loadUserPort.loadByEmail("buyer@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(VALID_COMMAND))
                .isInstanceOf(EmailAlreadyTakenException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    void register_publishesUserRegisteredEvent() {
        var saved = User.create("buyer@test.com", "bcrypt", "Jane", "Doe", "+5491111", Role.BUYER);
        when(loadUserPort.loadByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("bcrypt");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generate(any())).thenReturn("token");

        service.register(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("buyer@test.com");
        assertThat(captor.getValue().role()).isEqualTo(Role.BUYER);
    }

    @Test
    void register_encodesPasswordBeforeSaving() {
        var saved = User.create("buyer@test.com", "bcrypt-hash", "Jane", "Doe", "+5491111", Role.BUYER);
        when(loadUserPort.loadByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(saveUserPort.save(any())).thenReturn(saved);
        when(tokenGeneratorPort.generate(any())).thenReturn("token");

        service.register(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }
}
