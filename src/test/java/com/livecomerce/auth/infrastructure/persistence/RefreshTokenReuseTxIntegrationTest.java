package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.RefreshAccessTokenService;
import com.livecomerce.auth.application.RefreshTokenReuseException;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Integration test proving that revokeFamily COMMITS even when
 * RefreshTokenReuseException is thrown (BUG 1 regression guard).
 *
 * Uses a minimal Spring JPA context + H2 so no external DB is needed.
 * The real RefreshTokenPersistenceAdapter and Spring TX manager are used —
 * a plain Mockito unit test cannot catch this class of bug: the mock records
 * the revokeFamily() call regardless of transaction rollback.
 */
@SpringJUnitConfig(RefreshTokenReuseTxIntegrationTest.TestConfig.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-key-at-least-32-chars!!",
    "jwt.expiration-ms=900000",
    "jwt.refresh-expiration-ms=2592000000"
})
class RefreshTokenReuseTxIntegrationTest {

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
            // 30 days in ms — single source of truth matching jwt.refresh-expiration-ms
            return new JwtProperties("test-secret-key-at-least-32-chars!!", 900_000L, 30L * 24 * 60 * 60 * 1000);
        }

        @Bean
        RefreshTokenHasher refreshTokenHasher() {
            return new RefreshTokenHasher();
        }

        @Bean
        RefreshTokenPersistenceAdapter refreshTokenPersistenceAdapter(
                RefreshTokenJpaRepository repository,
                RefreshTokenHasher hasher) {
            return new RefreshTokenPersistenceAdapter(repository, hasher);
        }

        @Bean
        LoadUserPort loadUserPort() {
            return mock(LoadUserPort.class);
        }

        @Bean
        TokenGeneratorPort tokenGeneratorPort() {
            return mock(TokenGeneratorPort.class);
        }

        @Bean
        RefreshAccessTokenService refreshAccessTokenService(
                RefreshTokenStorePort store,
                LoadUserPort loadUser,
                TokenGeneratorPort tokenGenerator,
                RefreshTokenHasher hasher,
                JwtProperties props) {
            return new RefreshAccessTokenService(store, loadUser, tokenGenerator, hasher, props);
        }
    }

    @Autowired RefreshAccessTokenService service;
    @Autowired RefreshTokenJpaRepository repository;
    @Autowired RefreshTokenHasher hasher;
    @Autowired PlatformTransactionManager txManager;

    /**
     * RED before fix: revokeFamily is rolled back, tokens remain not-revoked.
     * GREEN after fix: noRollbackFor ensures revokeFamily commits despite the exception.
     */
    @Test
    void reuseDetection_revokesFamily_evenThoughExceptionPropagates() {
        UUID userId   = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        var tt = new TransactionTemplate(txManager);

        // 1. Persist a "parent" token for the family (already rotated → consumed)
        String consumedRaw  = hasher.generateRawToken();
        String consumedHash = hasher.hash(consumedRaw);
        tt.executeWithoutResult(s -> {
            RefreshToken consumed = RefreshToken.issue(userId, familyId, consumedHash, 30L * 24 * 60 * 60 * 1000);
            consumed.markUsed(); // simulate that this token was already rotated
            repository.save(consumed);

            // 2. Persist a "child" token still active in the same family
            String siblingHash = hasher.hash(hasher.generateRawToken());
            RefreshToken sibling = RefreshToken.issue(userId, familyId, siblingHash, 30L * 24 * 60 * 60 * 1000);
            repository.save(sibling);
        });

        // 3. Call refresh with the already-consumed token — must throw RefreshTokenReuseException.
        //    The service's transaction commits revokeFamily() before throwing (noRollbackFor).
        assertThatThrownBy(() -> service.refresh(consumedRaw))
                .isInstanceOf(RefreshTokenReuseException.class);

        // 4. Re-query in a new transaction and assert ALL tokens in the family are revoked.
        //    Without the noRollbackFor fix this assertion FAILS: revoked = false (rolled back).
        List<RefreshToken> family = tt.execute(s ->
            repository.findAll().stream()
                .filter(t -> familyId.equals(t.getFamilyId()))
                .toList()
        );

        assertThat(family).isNotEmpty();
        assertThat(family).allSatisfy(t ->
                assertThat(t.isRevoked())
                        .as("token %s in family %s must be revoked after reuse detection", t.getId(), familyId)
                        .isTrue()
        );
    }
}
