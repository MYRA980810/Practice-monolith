package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetProductRotationUseCase;
import com.livecomerce.analytics.application.port.out.LoadProductMetricsPort;
import com.livecomerce.analytics.domain.TopProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetProductRotationService implements GetProductRotationUseCase {

    private final LoadProductMetricsPort loadProductMetricsPort;

    @Override
    public List<TopProduct> getTopProducts(TopProductsQuery query) {
        return loadProductMetricsPort.loadTopProducts(
                query.storeId(), query.metric(), query.categoryId(),
                query.limit(), query.from(), query.to());
    }
}
