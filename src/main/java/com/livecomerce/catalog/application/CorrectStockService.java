package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CorrectStockUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.application.query.VariantView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CorrectStockService implements CorrectStockUseCase {

    private final LoadProductVariantPort loadProductVariantPort;
    private final SaveProductVariantPort saveProductVariantPort;

    @Override
    public VariantView correctStock(CorrectStockCommand command) {
        var variant = loadProductVariantPort.loadById(command.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.variantId()));

        if (!variant.getProduct().getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        variant.correctAvailableStock(command.availableQuantity());

        var saved = saveProductVariantPort.save(variant);
        return GetProductService.toVariantView(saved, saved.getProduct().getBasePrice());
    }
}
