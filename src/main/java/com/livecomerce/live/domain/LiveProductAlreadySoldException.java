package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveProductAlreadySoldException extends RuntimeException {

    public LiveProductAlreadySoldException(UUID liveProductId) {
        super("Live product is already sold and cannot be re-pinned: " + liveProductId);
    }
}
