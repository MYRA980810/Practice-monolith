package com.livecomerce.auth.application.port.in;

public interface AuthenticateUserUseCase {

    record AuthCommand(String email, String password) {}

    AuthResult authenticate(AuthCommand command);
}
