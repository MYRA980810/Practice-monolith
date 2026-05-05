package com.livecomerce.catalog;

import java.util.UUID;

public interface ReserveStockUseCase {

    record ReserveStockCommand(UUID productId, int quantity) {}

    void reserve(ReserveStockCommand command);
}
