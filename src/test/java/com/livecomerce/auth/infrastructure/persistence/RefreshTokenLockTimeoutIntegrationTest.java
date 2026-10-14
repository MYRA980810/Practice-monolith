package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.RefreshAccessTokenService;
import com.livecomerce.auth.application.RefreshTokenRetryException;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Proves that a refresh blocked on the token's row lock gives up after the database lock
 * timeout and surfaces as RefreshTokenRetryException (409) instead of waiting indefinitely.
 *
 * H2's LOCK_TIMEOUT stands in for the Postgres {@code SET LOCAL lock_timeout} issued by
 * {@link RowLockTimeout}: both are translated by Spring into the PessimisticLockingFailureException family.
 */
@SpringJUnitConfig(RefreshTokenLockTimeoutIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:refreshlocktimeoutdb;LOCK_TIMEOUT=200;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class RefreshTokenLockTimeoutIntegrationTest {

    private static final long TTL_MS = 30L * 24 * 60 * 60 * 1000;

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
        RefreshAccessTokenService refreshAccessTokenService(
                RefreshTokenStorePort store,
                RefreshTokenHasher hasher,
                JwtProperties props) {
            return new RefreshAccessTokenService(store, mock(LoadUserPort.class), mock(TokenGeneratorPort.class),
                    hasher, props, Clock.systemUTC());
        }
    }

    @Autowired RefreshAccessTokenService service;
    @Autowired RefreshTokenJpaRepository repository;
    @Autowired RefreshTokenHasher hasher;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void refreshBlockedOnRowLock_givesUpAfterLockTimeout_throwsRetry() throws Exception {
        var tt = new TransactionTemplate(txManager);
        String raw = hasher.generateRawToken();
        String hash = hasher.hash(raw);
        tt.executeWithoutResult(s ->
                repository.save(RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), hash, TTL_MS)));

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            // Simulates the winning refresh holding the row lock while it rotates the token.
            var holder = executor.submit(() -> tt.executeWithoutResult(s -> {
                repository.findForUpdateByTokenHash(hash).orElseThrow();
                locked.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> service.refresh(raw))
                    .isInstanceOf(RefreshTokenRetryException.class)
                    .hasCauseInstanceOf(PessimisticLockingFailureException.class);

            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        var token = tt.execute(s -> repository.findByTokenHash(hash).orElseThrow());
        assertThat(token.isConsumed()).as("timed-out refresh must not rotate the token").isFalse();
        assertThat(token.isRevoked()).isFalse();
    }
}
