package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStorePort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> loadByTokenHash(String tokenHash);

    void revokeFamily(UUID familyId);
}
