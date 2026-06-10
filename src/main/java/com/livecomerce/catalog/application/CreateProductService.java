package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {

    private final SaveProductPort saveProductPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Product create(CreateProductCommand command) {
        var product = Product.create(
                command.storeId(),
                command.name(),
                command.description(),
                command.basePrice(),
                command.currency(),
                command.sku(),
                command.categoryId()
        );

        var saved = saveProductPort.save(product);

        eventPublisher.publishEvent(
                new ProductCreatedEvent(saved.getId(), saved.getStoreId(), saved.getName()));

        return saved;
    }
}
