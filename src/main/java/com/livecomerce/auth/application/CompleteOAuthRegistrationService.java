package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.CompleteOAuthRegistrationUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteOAuthRegistrationService implements CompleteOAuthRegistrationUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    @Override
    public AuthResult complete(CompleteOAuthCommand command) {
        java.util.UUID userId;
        try {
            userId = tokenGeneratorPort.extractUserIdFromOAuthPendingToken(command.pendingToken());
        } catch (JwtException e) {
            throw new OAuthPendingTokenInvalidException();
        }

        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for OAuth pending token: " + userId));

        user.assignRole(command.role());
        var saved = saveUserPort.save(user);

        return AuthResult.of(
                tokenGeneratorPort.generate(saved),
                saved.getId(),
                saved.getEmail(),
                saved.getRole()
        ); // OAuth users always have email (from Google)
    }
}
