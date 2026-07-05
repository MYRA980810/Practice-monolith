package com.livecomerce.analytics.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class LiveSummaryNotFoundException extends DomainException {

    public LiveSummaryNotFoundException(UUID liveId) {
        super("No summary found for live " + liveId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/live-summary-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
