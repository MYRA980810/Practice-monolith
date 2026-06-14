package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ResumeProductUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeProductService implements ResumeProductUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public void resume(ResumeCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new AccessDeniedException("Product does not belong to this store");
        }

        product.resume();
        saveProductPort.save(product);
    }
}
