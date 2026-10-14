package com.livecomerce.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param refreshReuseGraceMs window after a refresh token is rotated during which presenting it again
 *                            (concurrent refresh from another tab) yields a retry instead of family revocation
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs, long refreshReuseGraceMs) {}
