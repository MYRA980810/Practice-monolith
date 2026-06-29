package com.livecomerce.live.application.port.out;

import java.util.UUID;

public interface ViewerCountPort {

    long increment(UUID liveId);

    long decrement(UUID liveId);

    long get(UUID liveId);
}
