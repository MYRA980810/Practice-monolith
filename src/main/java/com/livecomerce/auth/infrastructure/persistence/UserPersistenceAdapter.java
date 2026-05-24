package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository repository;

    @Override
    public Optional<User> loadByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public Optional<User> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> loadByProvider(String provider, String providerId) {
        return repository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }
}
