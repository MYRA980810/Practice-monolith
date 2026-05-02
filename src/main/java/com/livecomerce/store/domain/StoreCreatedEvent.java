package com.livecomerce.store.domain;

import java.util.UUID;

public record StoreCreatedEvent(UUID storeId, UUID userId, String slug) {}
