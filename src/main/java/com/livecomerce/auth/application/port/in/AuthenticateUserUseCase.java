package com.livecomerce.auth.application.port.in;

public interface AuthenticateUserUseCase {

    record AuthCommand(String contact, String password) {}

    AuthResult authenticate(AuthCommand command);
}
