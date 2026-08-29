package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
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
    public Optional<User> loadByPhone(String phone) {
        return repository.findByPhone(phone);
    }

    @Override
    public Optional<User> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @SuppressWarnings("null")
    public List<User> loadByIds(Collection<UUID> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public Optional<User> loadByProvider(String provider, String providerId) {
        return repository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public Optional<User> loadByAlias(String alias) {
        return repository.findByAlias(alias);
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }
}
