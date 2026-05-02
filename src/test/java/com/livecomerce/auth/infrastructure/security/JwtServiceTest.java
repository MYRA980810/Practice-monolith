package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 64 chars = 512 bits — minimum required by HMAC-SHA512
    private static final String SECRET = "test-secret-key-must-be-at-least-64-characters-long-for-hmacsha512";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 3_600_000L));
    }

    @Test
    void generate_producesTokenWithCorrectClaims() {
        var user = User.create("seller@test.com", "hash", "John", "Doe", null, Role.SELLER);

        var token = jwtService.generate(user);
        var claims = jwtService.validateAndExtract(token);

        assertThat(claims.get("email", String.class)).isEqualTo("seller@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("SELLER");
    }

    @Test
    void generate_producesNonBlankToken() {
        var user = User.create("buyer@test.com", "hash", "Jane", "Doe", null, Role.BUYER);

        assertThat(jwtService.generate(user)).isNotBlank();
    }

    @Test
    void validateAndExtract_withExpiredToken_throwsExpiredJwtException() {
        var expiredService = new JwtService(new JwtProperties(SECRET, -1000L));
        var user = User.create("x@x.com", "hash", "X", "X", null, Role.BUYER);
        var token = expiredService.generate(user);

        assertThatThrownBy(() -> jwtService.validateAndExtract(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateAndExtract_withTamperedToken_throwsException() {
        var user = User.create("x@x.com", "hash", "X", "X", null, Role.BUYER);
        var token = jwtService.generate(user) + "tampered";

        assertThatThrownBy(() -> jwtService.validateAndExtract(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validateAndExtract_withTokenSignedByDifferentSecret_throwsException() {
        var otherService = new JwtService(new JwtProperties(
                "other-secret-key-must-be-at-least-64-characters-long-for-hmacsha512", 3_600_000L
        ));
        var user = User.create("x@x.com", "hash", "X", "X", null, Role.BUYER);
        var token = otherService.generate(user);

        assertThatThrownBy(() -> jwtService.validateAndExtract(token))
                .isInstanceOf(Exception.class);
    }
}
