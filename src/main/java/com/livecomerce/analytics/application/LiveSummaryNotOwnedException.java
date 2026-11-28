package com.livecomerce.analytics.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class LiveSummaryNotOwnedException extends DomainException {

    public LiveSummaryNotOwnedException(UUID liveId, UUID sellerId) {
        super("Live summary %s does not belong to seller %s".formatted(liveId, sellerId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/live-summary-not-owned");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
