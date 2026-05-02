package com.livecomerce.catalog.domain;

import java.util.UUID;

public record ProductCreatedEvent(UUID productId, UUID storeId, String name) {}
