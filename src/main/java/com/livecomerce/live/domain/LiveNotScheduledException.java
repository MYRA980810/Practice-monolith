package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotScheduledException extends RuntimeException {

    public LiveNotScheduledException(UUID liveId) {
        super("Cannot subscribe: live session is not in SCHEDULED status: " + liveId);
    }
}
