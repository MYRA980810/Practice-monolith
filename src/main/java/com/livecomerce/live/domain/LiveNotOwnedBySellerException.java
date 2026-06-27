package com.livecomerce.live.domain;

import java.util.UUID;

public class LiveNotOwnedBySellerException extends RuntimeException {

    public LiveNotOwnedBySellerException(UUID liveId, UUID sellerId) {
        super("Live session " + liveId + " is not owned by seller " + sellerId);
    }
}
