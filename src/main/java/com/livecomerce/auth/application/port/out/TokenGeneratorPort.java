package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

import java.util.UUID;

public interface TokenGeneratorPort {
    String generate(User user);
    String generatePendingToken(User user);
    UUID extractUserIdFromPendingToken(String token);
    String generateOAuthPendingToken(User user);
    UUID extractUserIdFromOAuthPendingToken(String token);
    String generateResetPendingToken(User user);
    UUID extractUserIdFromResetPendingToken(String token);
    String generatePasswordResetToken(User user);
    UUID extractUserIdFromPasswordResetToken(String token);
}
