package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
class JwtService implements TokenGeneratorPort {

    private static final String TYPE_OTP_PENDING    = "otp-pending";
    private static final String TYPE_OAUTH_PENDING  = "oauth-pending";
    private static final String TYPE_RESET_PENDING  = "reset-pending";
    private static final String TYPE_PASSWORD_RESET = "password-reset";

    private final JwtProperties properties;
    private final SecretKey key;

    JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generate(User user) {
        if (user.getRole() == null) {
            throw new IllegalStateException("Cannot generate full JWT for user without a role: " + user.getId());
        }
        var now = new Date();
        var contact = user.getEmail() != null ? user.getEmail() : user.getPhone();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "contact", contact,
                        "role", user.getRole().name()
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.expirationMs()))
                .signWith(key)
                .compact();
    }

    @Override
    public String generatePendingToken(User user) {
        return generateTypedToken(user.getId(), TYPE_OTP_PENDING, 5L * 60 * 1000);
    }

    @Override
    public UUID extractUserIdFromPendingToken(String token) {
        return extractUserIdFromTypedToken(token, TYPE_OTP_PENDING);
    }

    @Override
    public String generateOAuthPendingToken(User user) {
        return generateTypedToken(user.getId(), TYPE_OAUTH_PENDING, 10L * 60 * 1000);
    }

    @Override
    public UUID extractUserIdFromOAuthPendingToken(String token) {
        return extractUserIdFromTypedToken(token, TYPE_OAUTH_PENDING);
    }

    @Override
    public String generateResetPendingToken(User user) {
        return generateTypedToken(user.getId(), TYPE_RESET_PENDING, 5L * 60 * 1000);
    }

    @Override
    public UUID extractUserIdFromResetPendingToken(String token) {
        return extractUserIdFromTypedToken(token, TYPE_RESET_PENDING);
    }

    @Override
    public String generatePasswordResetToken(User user) {
        return generateTypedToken(user.getId(), TYPE_PASSWORD_RESET, 15L * 60 * 1000);
    }

    @Override
    public UUID extractUserIdFromPasswordResetToken(String token) {
        return extractUserIdFromTypedToken(token, TYPE_PASSWORD_RESET);
    }

    Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateTypedToken(UUID userId, String type, long expirationMs) {
        var now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("type", type))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    private UUID extractUserIdFromTypedToken(String token, String expectedType) {
        var claims = validateAndExtract(token);
        var type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("Token type mismatch: expected '%s' but got '%s'".formatted(expectedType, type));
        }
        return UUID.fromString(claims.getSubject());
    }
}
