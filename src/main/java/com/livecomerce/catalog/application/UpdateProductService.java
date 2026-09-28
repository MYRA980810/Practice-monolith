package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateProductUseCase;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProductService implements UpdateProductUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;
    private final LoadCategoryPort loadCategoryPort;

    @Override
    public Product update(UpdateProductCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        product.update(
                command.name(),
                command.description(),
                command.basePrice(),
                command.currency(),
                command.sku()
        );

        if (command.categoryId() != null) {
            requireActiveCategory(command.categoryId());
            product.assignCategory(command.categoryId());
        }

        product.updateCompareAtPrice(command.compareAtPrice());

        return saveProductPort.save(product);
    }

    private void requireActiveCategory(UUID categoryId) {
        loadCategoryPort.loadById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotAvailableException(categoryId));
    }
}
