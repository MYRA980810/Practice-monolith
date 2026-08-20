package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
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

/**
 * Integration test for RefreshTokenPersistenceAdapter#revokeAllForUserExcept, using a
 * minimal Spring JPA context + H2 (mirrors RefreshTokenReuseTxIntegrationTest's pattern) so
 * the real JPQL query is exercised rather than a Mockito mock.
 */
@SpringJUnitConfig(RefreshTokenRevokeAllForUserExceptIntegrationTest.TestConfig.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:revoke_all_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class RefreshTokenRevokeAllForUserExceptIntegrationTest {

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
        RefreshTokenHasher refreshTokenHasher() {
            return new RefreshTokenHasher();
        }

        @Bean
        RefreshTokenPersistenceAdapter refreshTokenPersistenceAdapter(
                RefreshTokenJpaRepository repository,
                RefreshTokenHasher hasher) {
            return new RefreshTokenPersistenceAdapter(repository, hasher);
        }
    }

    @Autowired RefreshTokenPersistenceAdapter adapter;
    @Autowired RefreshTokenJpaRepository repository;
    @Autowired RefreshTokenHasher hasher;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void revokeAllForUserExcept_withExceptFamilyId_keepsThatFamilyRevokedFalseAndRevokesOthers() {
        var tt = new TransactionTemplate(txManager);
        UUID userId = UUID.randomUUID();
        UUID keptFamilyId = UUID.randomUUID();
        UUID otherFamilyId = UUID.randomUUID();

        tt.executeWithoutResult(s -> {
            var kept = RefreshToken.issue(userId, keptFamilyId, hasher.hash(hasher.generateRawToken()), 30L * 24 * 60 * 60 * 1000);
            var other = RefreshToken.issue(userId, otherFamilyId, hasher.hash(hasher.generateRawToken()), 30L * 24 * 60 * 60 * 1000);
            repository.save(kept);
            repository.save(other);
        });

        tt.executeWithoutResult(s -> adapter.revokeAllForUserExcept(userId, keptFamilyId));

        List<RefreshToken> tokens = tt.execute(s ->
                repository.findAll().stream().filter(t -> userId.equals(t.getUserId())).toList());

        assertThat(tokens).isNotEmpty();
        assertThat(tokens.stream().filter(t -> keptFamilyId.equals(t.getFamilyId())).findFirst())
                .get().satisfies(t -> assertThat(t.isRevoked()).isFalse());
        assertThat(tokens.stream().filter(t -> otherFamilyId.equals(t.getFamilyId())).findFirst())
                .get().satisfies(t -> assertThat(t.isRevoked()).isTrue());
    }

    @Test
    void revokeAllForUserExcept_withNullExceptFamilyId_revokesAllFamiliesForUser() {
        var tt = new TransactionTemplate(txManager);
        UUID userId = UUID.randomUUID();
        UUID familyOne = UUID.randomUUID();
        UUID familyTwo = UUID.randomUUID();

        tt.executeWithoutResult(s -> {
            var one = RefreshToken.issue(userId, familyOne, hasher.hash(hasher.generateRawToken()), 30L * 24 * 60 * 60 * 1000);
            var two = RefreshToken.issue(userId, familyTwo, hasher.hash(hasher.generateRawToken()), 30L * 24 * 60 * 60 * 1000);
            repository.save(one);
            repository.save(two);
        });

        tt.executeWithoutResult(s -> adapter.revokeAllForUserExcept(userId, null));

        List<RefreshToken> tokens = tt.execute(s ->
                repository.findAll().stream().filter(t -> userId.equals(t.getUserId())).toList());

        assertThat(tokens).isNotEmpty();
        assertThat(tokens).allSatisfy(t -> assertThat(t.isRevoked()).isTrue());
    }
}
