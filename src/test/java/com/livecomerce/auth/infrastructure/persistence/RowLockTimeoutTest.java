package com.livecomerce.auth.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RowLockTimeoutTest {

    @Mock EntityManager entityManager;
    @Mock Session session;
    @Mock Connection connection;
    @Mock DatabaseMetaData metaData;
    @Mock Statement statement;

    @BeforeEach
    void setUp() throws Exception {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(inv -> {
            inv.<Work>getArgument(0).execute(connection);
            return null;
        }).when(session).doWork(any());
        when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void applyToCurrentTransaction_onPostgres_setsLocalLockTimeout() throws Exception {
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.createStatement()).thenReturn(statement);

        new RowLockTimeout(entityManager, 3000L).applyToCurrentTransaction();

        verify(statement).execute("SET LOCAL lock_timeout = '3000ms'");
        verify(statement).close();
    }

    @Test
    void applyToCurrentTransaction_onH2_issuesNothing() throws Exception {
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        new RowLockTimeout(entityManager, 3000L).applyToCurrentTransaction();

        verify(connection, never()).createStatement();
    }

    @Test
    void applyToCurrentTransaction_detectsDatabaseOnlyOnce() throws Exception {
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.createStatement()).thenReturn(statement);
        var rowLockTimeout = new RowLockTimeout(entityManager, 3000L);

        rowLockTimeout.applyToCurrentTransaction();
        rowLockTimeout.applyToCurrentTransaction();

        verify(connection, times(1)).getMetaData();
        verify(statement, times(2)).execute("SET LOCAL lock_timeout = '3000ms'");
    }
}
