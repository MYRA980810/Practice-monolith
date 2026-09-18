package com.livecomerce.cart.application.port.in;

import java.util.UUID;

/** Removes a cart line. Idempotent — a no-op, not an error, if already absent. */
public interface RemoveFromCartUseCase {

    void removeLine(RemoveFromCartCommand command);

    record RemoveFromCartCommand(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {}
}
