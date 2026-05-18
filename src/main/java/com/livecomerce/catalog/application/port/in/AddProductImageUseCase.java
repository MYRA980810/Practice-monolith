package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface AddProductImageUseCase {

    record AddImageCommand(UUID productId, String url, Integer position, Boolean primary) {}

    Product addImage(AddImageCommand command);
}
