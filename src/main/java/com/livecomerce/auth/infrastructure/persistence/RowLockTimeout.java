package com.livecomerce.auth.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Bounds how long a {@code SELECT ... FOR UPDATE} in the current transaction may wait for a row lock.
 *
 * Hibernate's PostgreSQL dialect ignores positive {@code jakarta.persistence.lock.timeout} hints, so on
 * PostgreSQL this issues {@code SET LOCAL lock_timeout} on the transaction's own connection; it resets
 * automatically at commit/rollback. Other databases are left untouched (H2 tests configure LOCK_TIMEOUT
 * in the JDBC URL). On timeout PostgreSQL raises SQLState 55P03, which Spring translates into a
 * PessimisticLockingFailureException.
 */
@Component
class RowLockTimeout {

    private final EntityManager entityManager;
    private final long timeoutMs;
    private volatile Boolean postgres;

    RowLockTimeout(EntityManager entityManager,
                   @Value("${jwt.refresh-lock-timeout-ms:3000}") long timeoutMs) {
        this.entityManager = entityManager;
        this.timeoutMs = timeoutMs;
    }

    /** Must be called inside an active transaction, before the locking select. */
    void applyToCurrentTransaction() {
        entityManager.unwrap(Session.class).doWork(connection -> {
            if (!isPostgres(connection)) {
                return;
            }
            try (var statement = connection.createStatement()) {
                // SET does not accept bind parameters; timeoutMs is a long, so inlining it is safe.
                statement.execute("SET LOCAL lock_timeout = '" + timeoutMs + "ms'");
            }
        });
    }

    private boolean isPostgres(Connection connection) throws SQLException {
        var cached = postgres;
        if (cached == null) {
            cached = "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
            postgres = cached;
        }
        return cached;
    }
}
