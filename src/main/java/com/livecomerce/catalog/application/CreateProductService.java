package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {

    private final SaveProductPort saveProductPort;
    private final ApplicationEventPublisher eventPublisher;
    private final LoadCategoryPort loadCategoryPort;

    @Override
    public Product create(CreateProductCommand command) {
        requireActiveCategory(command.categoryId());

        var product = Product.create(
                command.storeId(),
                command.name(),
                command.description(),
                command.basePrice(),
                command.currency(),
                command.sku(),
                command.categoryId()
        );

        if (command.images() != null) {
            command.images().forEach(img -> product.addImage(img.url(), img.position(), img.primary()));
        }

        var saved = saveProductPort.save(product);

        eventPublisher.publishEvent(
                new ProductCreatedEvent(saved.getId(), saved.getStoreId(), saved.getName()));

        return saved;
    }

    private void requireActiveCategory(UUID categoryId) {
        loadCategoryPort.loadById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotAvailableException(categoryId));
    }
}
