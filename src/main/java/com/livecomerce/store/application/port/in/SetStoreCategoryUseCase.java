package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;
import jakarta.annotation.Nullable;

import java.util.UUID;

public interface SetStoreCategoryUseCase {

    /** Sets the seller's store category override; {@code null} clears it so inference applies again. */
    Store setCategory(UUID userId, @Nullable UUID categoryId);
}
