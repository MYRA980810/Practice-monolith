package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotLiveException extends RuntimeException {

    public static final String CODE = "LIVE_NOT_ACTIVE";

    public LiveNotLiveException(UUID id) {
        super("Live session is not currently active (status must be LIVE): " + id);
    }
}
