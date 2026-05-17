package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.RemoveProductImageUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RemoveProductImageService implements RemoveProductImageUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public Product removeImage(RemoveImageCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        try {
            product.removeImage(command.imageId());
        } catch (IllegalArgumentException e) {
            throw new ProductImageNotFoundException(command.imageId());
        }

        return saveProductPort.save(product);
    }
}
