package com.livecomerce.catalog.application;

import com.livecomerce.catalog.ReserveStockUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveStockService implements ReserveStockUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public void reserve(ReserveStockCommand command) {
        var product = loadProductPort.loadById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        product.reserveStock(command.quantity());

        saveProductPort.save(product);
    }
}
