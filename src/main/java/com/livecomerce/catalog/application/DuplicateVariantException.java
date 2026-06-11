package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

public class DuplicateVariantException extends DomainException {

    public DuplicateVariantException(UUID productId, Map<String, String> selection) {
        super("Duplicate variant combination for product %s: %s".formatted(productId, selection));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/duplicate-variant");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
