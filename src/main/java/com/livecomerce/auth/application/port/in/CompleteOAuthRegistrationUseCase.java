package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

public interface CompleteOAuthRegistrationUseCase {
    AuthResult complete(CompleteOAuthCommand command);

    record CompleteOAuthCommand(String pendingToken, Role role) {}
}
