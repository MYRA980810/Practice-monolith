package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface UpdateProductImageUseCase {

    record UpdateImageCommand(UUID productId, UUID storeId, UUID imageId, String url, int position, boolean primary) {}

    Product updateImage(UpdateImageCommand command);
}
