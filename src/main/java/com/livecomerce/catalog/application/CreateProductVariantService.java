package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductVariantUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.application.query.VariantView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductVariantService implements CreateProductVariantUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public VariantView createVariant(CreateProductVariantCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        product.addVariant(command.optionSelection(), command.sku(), command.priceOverride());

        var saved = saveProductPort.save(product);

        var variant = saved.getVariants().stream()
                .filter(v -> !v.isDefault())
                .reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("No non-default variant found after save"));

        return GetProductService.toVariantView(variant, saved.getBasePrice());
    }
}
