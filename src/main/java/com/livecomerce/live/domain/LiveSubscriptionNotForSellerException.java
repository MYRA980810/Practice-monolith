package com.livecomerce.live.domain;

public class LiveSubscriptionNotForSellerException extends RuntimeException {

    public LiveSubscriptionNotForSellerException() {
        super("Live subscriptions are for buyers only");
    }
}
