package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveProductOutOfStockException extends RuntimeException {

    private final UUID liveProductId;

    public LiveProductOutOfStockException(UUID liveProductId) {
        super("Live product out of stock: " + liveProductId);
        this.liveProductId = liveProductId;
    }

    public UUID getLiveProductId() {
        return liveProductId;
    }
}
