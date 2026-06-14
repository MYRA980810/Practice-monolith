package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.List;
import java.util.UUID;

public interface AddProductImagesUseCase {

    record ImageData(String url, Integer position, Boolean primary) {}

    record AddImagesCommand(UUID productId, UUID storeId, List<ImageData> images) {}

    Product addImages(AddImagesCommand command);
}
