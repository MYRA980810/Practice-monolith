package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ChangePasswordTokenResult;
import com.livecomerce.auth.application.port.in.VerifyChangePasswordOtpUseCase;
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
public class VerifyChangePasswordOtpService implements VerifyChangePasswordOtpUseCase {

    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public ChangePasswordTokenResult verify(VerifyChangePasswordOtpCommand command) {
        UUID userId;
        try {
            userId = tokenGeneratorPort.extractUserIdFromChangePasswordPendingToken(command.pendingToken());
        } catch (Exception e) {
            throw new ChangePasswordPendingTokenInvalidException();
        }
        if (!userId.equals(command.userId())) {
            throw new ChangePasswordPendingTokenInvalidException();
        }
        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + userId));
        boolean valid = verifyOtpPort.check(user.resolveRecipient(), command.code(), user.resolveChannel());
        if (!valid) throw new InvalidOtpException();
        return new ChangePasswordTokenResult(tokenGeneratorPort.generateChangePasswordToken(user));
    }
}
