package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ChangePasswordUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService implements ChangePasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStorePort refreshTokenStorePort;
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    public void changePassword(ChangePasswordCommand command) {
        UUID userId;
        try {
            userId = tokenGeneratorPort.extractUserIdFromChangePasswordToken(command.changePasswordToken());
        } catch (Exception e) {
            throw new ChangePasswordTokenInvalidException();
        }
        if (!userId.equals(command.userId())) {
            throw new ChangePasswordTokenInvalidException();
        }
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        var user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + userId));
        user.updatePasswordHash(passwordEncoder.encode(command.newPassword()));
        saveUserPort.save(user);

        UUID currentFamilyId = null;
        var currentRefreshToken = command.currentRefreshToken();
        if (currentRefreshToken != null && !currentRefreshToken.isBlank()) {
            var hash = refreshTokenHasher.hash(currentRefreshToken);
            currentFamilyId = refreshTokenStorePort.loadByTokenHash(hash)
                    .map(token -> token.getFamilyId())
                    .orElse(null);
        }
        refreshTokenStorePort.revokeAllForUserExcept(command.userId(), currentFamilyId);
    }
}
