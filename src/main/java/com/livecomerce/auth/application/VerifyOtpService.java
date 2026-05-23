package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.VerifyOtpUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.VerificationChannel;
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

        var channel = user.getPhone() != null ? VerificationChannel.WHATSAPP : VerificationChannel.EMAIL;
        var recipientAddress = channel == VerificationChannel.WHATSAPP ? user.getPhone() : user.getEmail();

        if (!verifyOtpPort.check(recipientAddress, command.code())) {
            throw new InvalidOtpException();
        }

        user.verify();
        var saved = saveUserPort.save(user);

        return AuthResult.of(tokenGeneratorPort.generate(saved), saved.getId(), saved.getEmail(), saved.getRole());
    }

    private java.util.UUID extractUserId(String pendingToken) {
        try {
            return tokenGeneratorPort.extractUserIdFromPendingToken(pendingToken);
        } catch (Exception e) {
            throw new PendingTokenInvalidException();
        }
    }
}
