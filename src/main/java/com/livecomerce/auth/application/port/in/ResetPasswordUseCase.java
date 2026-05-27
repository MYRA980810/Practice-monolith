package com.livecomerce.auth.application.port.in;

public interface ResetPasswordUseCase {
    record ResetPasswordCommand(String resetToken, String newPassword, String confirmPassword) {}
    AuthResult reset(ResetPasswordCommand command);
}
