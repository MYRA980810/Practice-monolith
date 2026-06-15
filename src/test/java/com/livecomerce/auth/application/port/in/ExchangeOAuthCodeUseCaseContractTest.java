package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeOAuthCodeUseCaseContractTest {

    /**
     * Compile-only contract test: verifies exchange(ExchangeCommand) signature
     * and nested ExchangeCommand(String code) record exist.
     */
    @Test
    void interface_implementableWithExchangeMethod() {
        ExchangeOAuthCodeUseCase impl = command ->
                AuthResult.of("jwt-token", UUID.randomUUID(), "user@test.com", Role.SELLER, null, null);

        var command = new ExchangeOAuthCodeUseCase.ExchangeCommand("some-code");
        assertThat(command.code()).isEqualTo("some-code");

        var result = impl.exchange(command);
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("jwt-token");
    }
}
