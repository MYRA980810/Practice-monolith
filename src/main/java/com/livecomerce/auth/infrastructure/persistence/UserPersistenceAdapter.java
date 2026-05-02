package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository repository;

    @Override
    public Optional<User> loadByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }
}
