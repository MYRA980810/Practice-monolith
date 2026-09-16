package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface UpdateOptionValueSwatchUseCase {

    record UpdateOptionValueSwatchCommand(
            UUID productId,
            UUID storeId,
            UUID optionId,
            UUID valueId,
            String swatchHex
    ) {}

    Product updateSwatch(UpdateOptionValueSwatchCommand command);
}
