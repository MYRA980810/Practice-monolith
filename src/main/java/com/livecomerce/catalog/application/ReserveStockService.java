package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ReserveStockUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveStockService implements ReserveStockUseCase {

    private final LoadProductVariantPort loadProductVariantPort;
    private final SaveProductVariantPort saveProductVariantPort;

    @Override
    public void reserve(ReserveStockCommand command) {
        var variant = loadProductVariantPort.loadById(command.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.variantId()));

        if (!variant.canReserve(command.quantity())) {
            throw new InsufficientStockException(
                    command.variantId(),
                    command.quantity(),
                    variant.getStock().getAvailableQuantity());
        }

        variant.reserveStock(command.quantity());

        saveProductVariantPort.save(variant);
    }
}
