package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.List;
import java.util.UUID;

public interface AddProductOptionUseCase {

    record AddProductOptionCommand(UUID productId, UUID storeId, String name, List<String> values) {}

    Product addOption(AddProductOptionCommand command);
}
