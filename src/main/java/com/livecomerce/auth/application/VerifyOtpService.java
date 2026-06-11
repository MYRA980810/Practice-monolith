package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.VerifyOtpUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VerifyOtpService implements VerifyOtpUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public AuthResult verify(VerifyCommand command) {
        var userId = extractUserId(command.pendingToken());

        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        var channel = user.resolveChannel();
        var recipientAddress = user.resolveRecipient();

        if (!verifyOtpPort.check(recipientAddress, command.code(), channel)) {
            throw new InvalidOtpException();
        }

        user.verify();
        var saved = saveUserPort.save(user);

        var contact = saved.getEmail() != null ? saved.getEmail() : saved.getPhone();
        return AuthResult.of(tokenGeneratorPort.generate(saved), saved.getId(), contact, saved.getRole(), saved.getAvatarUrl());
    }

    private java.util.UUID extractUserId(String pendingToken) {
        try {
            return tokenGeneratorPort.extractUserIdFromPendingToken(pendingToken);
        } catch (Exception e) {
            throw new PendingTokenInvalidException();
        }
    }
}
