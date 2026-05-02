package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface AddStockUseCase {

    record AddStockCommand(UUID productId, int quantity) {}

    Product addStock(AddStockCommand command);
}
