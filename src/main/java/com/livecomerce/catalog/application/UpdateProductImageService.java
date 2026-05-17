package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase;
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
public class UpdateProductImageService implements UpdateProductImageUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public Product updateImage(UpdateImageCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        try {
            product.updateImage(command.imageId(), command.url(), command.position(), command.primary());
        } catch (IllegalArgumentException e) {
            throw new ProductImageNotFoundException(command.imageId());
        }

        return saveProductPort.save(product);
    }
}
