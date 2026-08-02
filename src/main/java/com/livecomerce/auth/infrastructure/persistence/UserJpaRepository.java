package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByAlias(String alias);
}
