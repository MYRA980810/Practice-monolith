package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
class JwtService implements TokenGeneratorPort {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties properties;

    @Override
    public String generate(User user) {
        var key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        var now = new Date();

        var token = Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.expirationMs()))
                .signWith(key)
                .compact();

        log.info("Generated token (last 20 chars): ...{}", token.substring(token.length() - 20));
        log.info("Secret length used for signing: {} bytes", properties.secret().getBytes(StandardCharsets.UTF_8).length);
        return token;
    }

    Claims validateAndExtract(String token) {
        log.info("Validating token (last 20 chars): ...{}", token.substring(Math.max(0, token.length() - 20)));
        log.info("Secret length used for validation: {} bytes", properties.secret().getBytes(StandardCharsets.UTF_8).length);
        var key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
