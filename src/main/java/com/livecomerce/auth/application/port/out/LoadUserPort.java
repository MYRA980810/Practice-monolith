package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadByEmail(String email);
}
