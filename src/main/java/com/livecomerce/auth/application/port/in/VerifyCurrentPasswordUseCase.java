package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface VerifyCurrentPasswordUseCase {
    record VerifyCurrentPasswordCommand(UUID userId, String currentPassword) {}
    PendingVerificationResult verifyCurrentPassword(VerifyCurrentPasswordCommand command);
}
