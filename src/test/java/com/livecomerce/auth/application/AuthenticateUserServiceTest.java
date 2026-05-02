package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase.AuthCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthenticateUserService service;

    @Test
    void authenticate_withValidCredentials_returnsAuthResult() {
        var user = User.create("seller@test.com", "hash", "John", "Doe", "+1234", Role.SELLER);
        when(loadUserPort.loadByEmail("seller@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tokenGeneratorPort.generate(user)).thenReturn("jwt-token");

        var result = service.authenticate(new AuthCommand("seller@test.com", "secret"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("seller@test.com");
        assertThat(result.role()).isEqualTo(Role.SELLER);
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void authenticate_whenUserNotFound_throwsInvalidCredentials() {
        when(loadUserPort.loadByEmail("x@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(new AuthCommand("x@x.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_whenUserInactive_throwsInvalidCredentials() {
        var user = User.create("seller@test.com", "hash", "John", "Doe", "+1234", Role.SELLER);
        user.deactivate();
        when(loadUserPort.loadByEmail("seller@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate(new AuthCommand("seller@test.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_whenPasswordDoesNotMatch_throwsInvalidCredentials() {
        var user = User.create("seller@test.com", "hash", "John", "Doe", "+1234", Role.SELLER);
        when(loadUserPort.loadByEmail("seller@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(new AuthCommand("seller@test.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
