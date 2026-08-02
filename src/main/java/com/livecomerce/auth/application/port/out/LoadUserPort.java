package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {
    Optional<User> loadByEmail(String email);
    Optional<User> loadByPhone(String phone);
    Optional<User> loadById(UUID id);
    Optional<User> loadByProvider(String provider, String providerId);
    Optional<User> loadByAlias(String alias);
}
