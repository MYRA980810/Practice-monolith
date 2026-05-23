package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResult authenticate(AuthCommand command) {
        var user = loadUserPort.loadByEmail(command.email())
                .filter(u -> u.isActive())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new AccountNotVerifiedException();
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return AuthResult.of(tokenGeneratorPort.generate(user), user.getId(), user.getEmail(), user.getRole());
    }
}
