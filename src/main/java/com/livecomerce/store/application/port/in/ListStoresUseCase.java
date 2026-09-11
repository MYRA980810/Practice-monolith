package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ListStoresUseCase {

    Page<Store> listActive(Pageable pageable);

    /**
     * Ids only (no {@code Store} in the return type) so modules whose
     * {@code allowedDependencies} only grant {@code store::in} — not
     * {@code store}'s domain package — can call this directly, e.g. the
     * nightly ranking job in {@code analytics}.
     */
    List<UUID> listActiveIds();
}
