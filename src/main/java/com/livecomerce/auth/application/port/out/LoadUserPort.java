package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {
    Optional<User> loadByEmail(String email);
    Optional<User> loadById(UUID id);
}
