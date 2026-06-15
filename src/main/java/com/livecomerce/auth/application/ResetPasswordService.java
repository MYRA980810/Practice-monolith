package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.ResetPasswordUseCase;
import com.livecomerce.auth.application.port.out.IssueRefreshTokenPort;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService implements ResetPasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoder passwordEncoder;
    private final IssueRefreshTokenPort issueRefreshTokenPort;

    @Override
    public AuthResult reset(ResetPasswordCommand command) {
        UUID userId;
        try {
            userId = tokenGeneratorPort.extractUserIdFromPasswordResetToken(command.resetToken());
        } catch (Exception e) {
            throw new PasswordResetTokenInvalidException();
        }
        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + userId));
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        user.updatePasswordHash(passwordEncoder.encode(command.newPassword()));
        var saved = saveUserPort.save(user);
        var contact = saved.getEmail() != null ? saved.getEmail() : saved.getPhone();
        var rawRefreshToken = issueRefreshTokenPort.issueForUser(saved.getId());
        return AuthResult.of(tokenGeneratorPort.generate(saved), saved.getId(), contact, saved.getRole(), saved.getAvatarUrl(), rawRefreshToken);
    }
}
