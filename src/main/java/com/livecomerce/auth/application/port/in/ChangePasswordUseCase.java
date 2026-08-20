package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {
    record ChangePasswordCommand(UUID userId, String changePasswordToken, String newPassword,
                                  String confirmPassword, String currentRefreshToken) {}
    void changePassword(ChangePasswordCommand command);
}
