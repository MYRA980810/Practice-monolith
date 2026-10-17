package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.RefreshAccessTokenService;
import com.livecomerce.auth.application.RefreshTokenInvalidException;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Proves that a refresh for a user that is inactive or no longer exists revokes the whole token
 * family and that the revocation COMMITS while {@link RefreshTokenInvalidException} propagates as
 * 401. Same minimal JPA + H2 context as {@link RefreshTokenReuseTxIntegrationTest}, but with the
 * real user adapter, so the user lookup goes through the database too. A Mockito unit test can't
 * catch a rollback: the mock records revokeFamily() whether or not it was rolled back.
 */
@SpringJUnitConfig(RefreshForUnusableUserTxIntegrationTest.TestConfig.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:refresh-unusable-user;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class RefreshForUnusableUserTxIntegrationTest {

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
        UserPersistenceAdapter userPersistenceAdapter(UserJpaRepository repository) {
            return new UserPersistenceAdapter(repository);
        }

        @Bean
        TokenGeneratorPort tokenGeneratorPort() {
            return mock(TokenGeneratorPort.class);
        }

        @Bean
        RefreshAccessTokenService refreshAccessTokenService(
                RefreshTokenStorePort store,
                UserPersistenceAdapter users,
                TokenGeneratorPort tokenGenerator,
                RefreshTokenHasher hasher,
                JwtProperties props) {
            return new RefreshAccessTokenService(store, users, tokenGenerator, hasher, props, Clock.systemUTC());
        }
    }

    @Autowired RefreshAccessTokenService service;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired UserJpaRepository users;
    @Autowired RefreshTokenHasher hasher;
    @Autowired TokenGeneratorPort tokenGenerator;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void refreshOfInactiveUser_throwsInvalid_andTheRevokedFamilyIsCommitted() {
        var user = User.create("inactive-" + UUID.randomUUID() + "@test.com", "hash", "Jane", "Doe",
                null, Role.BUYER);
        user.deactivate();
        tx().executeWithoutResult(s -> users.save(user));

        assertRefreshRevokesFamilyDurably(user.getId());
    }

    /** refresh_tokens.user_id has no FK to users (V21), so a token can outlive its user. */
    @Test
    void refreshOfTokenWhoseUserNoLongerExists_throwsInvalid_andTheRevokedFamilyIsCommitted() {
        assertRefreshRevokesFamilyDurably(UUID.randomUUID());
    }

    private void assertRefreshRevokesFamilyDurably(UUID userId) {
        var familyId = UUID.randomUUID();
        var presentedRaw = hasher.generateRawToken();
        tx().executeWithoutResult(s -> {
            refreshTokens.save(RefreshToken.issue(userId, familyId, hasher.hash(presentedRaw), TTL_MS));
            // Another live session of the same family, which must die too.
            refreshTokens.save(RefreshToken.issue(userId, familyId, hasher.hash(hasher.generateRawToken()), TTL_MS));
        });

        assertThatThrownBy(() -> service.refresh(presentedRaw))
                .isInstanceOf(RefreshTokenInvalidException.class);

        // Re-read in a new transaction: without noRollbackFor the revocation is rolled back.
        var family = tx().execute(s -> refreshTokens.findAll().stream()
                .filter(t -> familyId.equals(t.getFamilyId()))
                .toList());

        assertThat(family).hasSize(2);
        assertThat(family).allSatisfy(t -> assertThat(t.isRevoked())
                .as("token %s of family %s must be revoked", t.getId(), familyId)
                .isTrue());
        assertThat(family).allSatisfy(t -> assertThat(t.getReplacedBy()).isNull());
        verifyNoInteractions(tokenGenerator);
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }
}
