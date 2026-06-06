package com.livecomerce.auth.application.port.in;

public interface ExchangeOAuthCodeUseCase {
    AuthResult exchange(ExchangeCommand command);

    record ExchangeCommand(String code) {
    }
}
