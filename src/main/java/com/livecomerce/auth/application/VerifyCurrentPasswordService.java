package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.PendingVerificationResult;
import com.livecomerce.auth.application.port.in.VerifyCurrentPasswordUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VerifyCurrentPasswordService implements VerifyCurrentPasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoder passwordEncoder;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public PendingVerificationResult verifyCurrentPassword(VerifyCurrentPasswordCommand command) {
        var user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + command.userId()));
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new CurrentPasswordInvalidException();
        }
        var channel = user.resolveChannel();
        verifyOtpPort.send(user.resolveRecipient(), channel);
        return new PendingVerificationResult(tokenGeneratorPort.generateChangePasswordPendingToken(user), channel);
    }
}
