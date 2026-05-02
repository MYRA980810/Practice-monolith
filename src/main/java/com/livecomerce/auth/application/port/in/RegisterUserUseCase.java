package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

public interface RegisterUserUseCase {

    record RegisterCommand(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            Role role
    ) {}

    AuthResult register(RegisterCommand command);
}
