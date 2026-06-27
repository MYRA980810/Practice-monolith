package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveProductNotFoundException extends RuntimeException {

    public LiveProductNotFoundException(UUID id) {
        super("Live product not found: " + id);
    }
}
