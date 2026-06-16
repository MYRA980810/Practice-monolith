package com.livecomerce.order;

import java.util.UUID;

public interface StockReservationPort {

    record ReserveStockCommand(UUID variantId, int quantity) {}

    void reserve(ReserveStockCommand command);
}
