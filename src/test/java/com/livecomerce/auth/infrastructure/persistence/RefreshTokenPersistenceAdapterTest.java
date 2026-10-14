package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenPersistenceAdapterTest {

    @Mock RefreshTokenJpaRepository repository;
    @Mock RefreshTokenHasher refreshTokenHasher;
    @Mock RowLockTimeout rowLockTimeout;

    @Test
    void loadByTokenHashForUpdate_boundsLockWaitBeforeLockingSelect() {
        when(repository.findForUpdateByTokenHash("hash")).thenReturn(Optional.empty());
        var adapter = new RefreshTokenPersistenceAdapter(repository, refreshTokenHasher, rowLockTimeout);

        adapter.loadByTokenHashForUpdate("hash");

        InOrder order = inOrder(rowLockTimeout, repository);
        order.verify(rowLockTimeout).applyToCurrentTransaction();
        order.verify(repository).findForUpdateByTokenHash("hash");
    }
}
