package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.ExchangeOAuthCodeUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import com.livecomerce.auth.application.port.out.OAuthTokenType;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeOAuthCodeService implements ExchangeOAuthCodeUseCase {

    private final OAuthCodeStorePort codeStorePort;
    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    @Override
    public AuthResult exchange(ExchangeCommand command) {
        var payload = codeStorePort.exchange(command.code())
                .orElseThrow(OAuthCodeInvalidException::new);

        var user = loadUserPort.loadById(payload.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "User not found for OAuth code payload: " + payload.userId()));

        var jwt = payload.tokenType() == OAuthTokenType.FULL
                ? tokenGeneratorPort.generate(user)
                : tokenGeneratorPort.generateOAuthPendingToken(user);

        return AuthResult.of(jwt, user.getId(), user.getEmail(), user.getRole());
    }
}
