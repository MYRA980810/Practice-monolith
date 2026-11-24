package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotFoundException extends RuntimeException {

    public static final String CODE = "LIVE_NOT_FOUND";

    public LiveNotFoundException(UUID id) {
        super("Live session not found: " + id);
    }
}
