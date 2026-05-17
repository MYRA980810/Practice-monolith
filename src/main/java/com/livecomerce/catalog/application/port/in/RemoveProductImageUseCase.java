package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface RemoveProductImageUseCase {

    record RemoveImageCommand(UUID productId, UUID storeId, UUID imageId) {}

    Product removeImage(RemoveImageCommand command);
}
