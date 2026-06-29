package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotLiveException extends RuntimeException {

    public LiveNotLiveException(UUID id) {
        super("Live session is not currently active (status must be LIVE): " + id);
    }
}
