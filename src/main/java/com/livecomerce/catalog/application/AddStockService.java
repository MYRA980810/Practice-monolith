package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.application.query.VariantView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddStockService implements AddStockUseCase {

    private final LoadProductVariantPort loadProductVariantPort;
    private final SaveProductVariantPort saveProductVariantPort;

    @Override
    public VariantView addStock(AddStockCommand command) {
        var variant = loadProductVariantPort.loadById(command.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.variantId()));

        variant.addStock(command.quantity());

        var saved = saveProductVariantPort.save(variant);
        return GetProductService.toVariantView(saved, saved.getProduct().getBasePrice());
    }
}
