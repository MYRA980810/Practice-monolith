package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.RefreshAccessTokenService;
import com.livecomerce.auth.application.RefreshTokenRetryException;
import com.livecomerce.auth.application.port.in.RefreshAccessTokenUseCase.RefreshResult;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.infrastructure.security.JwtProperties;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test proving that two concurrent refreshes of the same refresh token
 * (e.g. two browser tabs sharing the cookie) do NOT trip reuse detection:
 * the pessimistic row lock serializes them, one rotates the token and the other
 * gets RefreshTokenRetryException inside the grace window, and the family stays alive.
 *
 * The token generator mock sleeps while the winning transaction still holds the row lock,
 * so the losing thread reliably blocks on SELECT ... FOR UPDATE. Without the lock both
 * threads would read the token as unconsumed and both would rotate it.
 *
 * No @AutoConfigureTestDatabase here: it would swap in a random embedded URL and drop the
 * explicit H2 LOCK_TIMEOUT, which must comfortably exceed the time the lock is held.
 */
@SpringJUnitConfig(RefreshTokenConcurrentRefreshIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:refreshconcurrencydb;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class RefreshTokenConcurrentRefreshIntegrationTest {

    private static final long TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long HOLD_LOCK_MS = 500L;

    @Configuration
    @EnableJpaRepositories(basePackageClasses = RefreshTokenJpaRepository.class)
    @EntityScan(basePackageClasses = RefreshToken.class)
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class
    })
    static class TestConfig {

        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties("test-secret-key-at-least-32-chars!!", 900_000L, TTL_MS, 30_000L);
        }

        @Bean
        RefreshTokenHasher refreshTokenHasher() {
            return new RefreshTokenHasher();
        }

        @Bean
        RefreshTokenPersistenceAdapter refreshTokenPersistenceAdapter(
                RefreshTokenJpaRepository repository,
                RefreshTokenHasher hasher,
                EntityManager entityManager) {
            return new RefreshTokenPersistenceAdapter(repository, hasher, new RowLockTimeout(entityManager, 3000L));
        }

        @Bean
        LoadUserPort loadUserPort() {
            var port = mock(LoadUserPort.class);
            var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.BUYER);
            when(port.loadById(any())).thenReturn(Optional.of(user));
            return port;
        }

        @Bean
        TokenGeneratorPort tokenGeneratorPort() {
            var port = mock(TokenGeneratorPort.class);
            // Runs inside the refresh transaction, after the row lock was taken:
            // keeps the lock held long enough for the competing thread to block on it.
            when(port.generate(any())).thenAnswer(inv -> {
                Thread.sleep(HOLD_LOCK_MS);
                return "new-access-token";
            });
            return port;
        }

        @Bean
        RefreshAccessTokenService refreshAccessTokenService(
                RefreshTokenStorePort store,
                LoadUserPort loadUser,
                TokenGeneratorPort tokenGenerator,
                RefreshTokenHasher hasher,
                JwtProperties props) {
            return new RefreshAccessTokenService(store, loadUser, tokenGenerator, hasher, props, Clock.systemUTC());
        }
    }

    @Autowired RefreshAccessTokenService service;
    @Autowired RefreshTokenJpaRepository repository;
    @Autowired RefreshTokenHasher hasher;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void concurrentRefreshOfSameToken_oneRotatesOtherGetsRetry_familyStaysActive() throws Exception {
        UUID userId   = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        var tt = new TransactionTemplate(txManager);

        String raw = hasher.generateRawToken();
        tt.executeWithoutResult(s ->
                repository.save(RefreshToken.issue(userId, familyId, hasher.hash(raw), TTL_MS)));

        var start = new CountDownLatch(1);
        Callable<RefreshResult> call = () -> {
            start.await();
            return service.refresh(raw);
        };

        var executor = Executors.newFixedThreadPool(2);
        List<RefreshResult> successes = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        try {
            Future<RefreshResult> first  = executor.submit(call);
            Future<RefreshResult> second = executor.submit(call);
            start.countDown();

            for (var future : List.of(first, second)) {
                try {
                    successes.add(future.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(RefreshTokenRetryException.class);

        // Re-query in a new transaction: the original is rotated, its successor is still active.
        List<RefreshToken> family = tt.execute(s ->
                repository.findAll().stream()
                        .filter(t -> familyId.equals(t.getFamilyId()))
                        .toList());

        assertThat(family).hasSize(2);
        var original = family.stream().filter(t -> t.getTokenHash().equals(hasher.hash(raw))).findFirst().orElseThrow();
        var successor = family.stream().filter(t -> t.getId().equals(original.getReplacedBy())).findFirst().orElseThrow();
        assertThat(original.isConsumed()).isTrue();
        assertThat(successor.isRevoked())
                .as("successor must stay active: concurrent refresh must not revoke the family")
                .isFalse();
        assertThat(successor.getTokenHash()).isEqualTo(hasher.hash(successes.get(0).refreshToken()));
    }
}
