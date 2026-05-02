package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface AddProductImageUseCase {

    record AddImageCommand(UUID productId, String url, int position, boolean primary) {}

    Product addImage(AddImageCommand command);
}
