package com.livecomerce.auth.application.port.in;

public interface VerifyResetCodeUseCase {
    record VerifyResetCommand(String pendingToken, String code) {}
    ResetTokenResult verify(VerifyResetCommand command);
}
