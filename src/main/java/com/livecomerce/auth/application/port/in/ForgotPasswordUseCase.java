package com.livecomerce.auth.application.port.in;

public interface ForgotPasswordUseCase {
    record ForgotPasswordCommand(String contact) {}
    PendingVerificationResult forgotPassword(ForgotPasswordCommand command);
}
