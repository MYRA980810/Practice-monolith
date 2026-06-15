package com.livecomerce.auth.application;

import com.livecomerce.auth.api.validation.ContactValidator;
import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.out.IssueRefreshTokenPort;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final IssueRefreshTokenPort issueRefreshTokenPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResult authenticate(AuthCommand command) {
        var contact = command.contact();
        var lookup = ContactValidator.isPhone(contact)
                ? loadUserPort.loadByPhone(contact)
                : loadUserPort.loadByEmail(contact);
        var user = lookup
                .filter(u -> u.isActive())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new AccountNotVerifiedException();
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        var resolvedContact = user.getEmail() != null ? user.getEmail() : user.getPhone();
        var rawRefreshToken = issueRefreshTokenPort.issueForUser(user.getId());
        return AuthResult.of(tokenGeneratorPort.generate(user), user.getId(), resolvedContact, user.getRole(), user.getAvatarUrl(), rawRefreshToken);
    }
}
