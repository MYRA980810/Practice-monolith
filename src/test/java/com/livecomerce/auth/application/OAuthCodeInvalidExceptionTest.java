package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCodeInvalidExceptionTest {

    @Test
    void exception_isInstanceOfDomainException() {
        assertThat(new OAuthCodeInvalidException())
                .isInstanceOf(DomainException.class);
    }

    @Test
    void exception_statusIsUnauthorized() {
        assertThat(new OAuthCodeInvalidException().getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exception_typeContainsOauthCodeInvalid() {
        assertThat(new OAuthCodeInvalidException().getType().toString())
                .contains("oauth-code-invalid");
    }
}
