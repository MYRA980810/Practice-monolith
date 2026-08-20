package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ResendOtpUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ResendOtpService implements ResendOtpUseCase {

    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public void resend(ResendCommand command) {
        var userId = extractUserId(command.pendingToken());

        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for pending token: " + userId));

        verifyOtpPort.send(user.resolveRecipient(), user.resolveChannel());
    }

    private UUID extractUserId(String pendingToken) {
        try {
            return tokenGeneratorPort.extractUserIdFromAnyPendingToken(pendingToken);
        } catch (Exception e) {
            throw new PendingTokenInvalidException();
        }
    }
}
