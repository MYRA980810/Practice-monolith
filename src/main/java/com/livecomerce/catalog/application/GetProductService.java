package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {

    private final LoadProductPort loadProductPort;

    @Override
    public Product getById(UUID productId) {
        return loadProductPort.loadById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public List<Product> getByStoreId(UUID storeId) {
        return loadProductPort.loadByStoreId(storeId);
    }
}
