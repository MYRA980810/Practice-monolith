package com.livecomerce.catalog.application.port.in;

import java.util.UUID;

public interface ReserveStockUseCase {

    record ReserveStockCommand(UUID productId, int quantity) {}

    void reserve(ReserveStockCommand command);
}
