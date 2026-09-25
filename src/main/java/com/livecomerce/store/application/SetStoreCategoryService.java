package com.livecomerce.store.application;

import com.livecomerce.store.StoreCategoryPort;
import com.livecomerce.store.application.port.in.SetStoreCategoryUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SetStoreCategoryService implements SetStoreCategoryUseCase {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;
    private final StoreCategoryPort storeCategoryPort;

    @Override
    public Store setCategory(UUID userId, @Nullable UUID categoryId) {
        Store store = loadStorePort.loadByUserId(userId)
                .orElseThrow(() -> new StoreNotFoundException(userId.toString()));

        // null clears the override (inference applies again), so it needs no validation.
        if (categoryId != null && !storeCategoryPort.isActive(categoryId)) {
            throw new InvalidStoreCategoryException(categoryId);
        }

        store.changeCategory(categoryId);

        return saveStorePort.save(store);
    }
}
