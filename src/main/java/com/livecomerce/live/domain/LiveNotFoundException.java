package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotFoundException extends RuntimeException {

    public LiveNotFoundException(UUID id) {
        super("Live session not found: " + id);
    }
}
