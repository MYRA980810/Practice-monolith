package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

public interface RegisterUserUseCase {

    record RegisterCommand(
            String contact,
            String password,
            String firstName,
            String lastName,
            Role role
    ) {}

    PendingVerificationResult register(RegisterCommand command);
}
