package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface VerifyChangePasswordOtpUseCase {
    record VerifyChangePasswordOtpCommand(UUID userId, String pendingToken, String code) {}
    ChangePasswordTokenResult verify(VerifyChangePasswordOtpCommand command);
}
