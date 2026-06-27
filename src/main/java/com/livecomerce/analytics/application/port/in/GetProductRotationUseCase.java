package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.TopProduct;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface GetProductRotationUseCase {

    List<TopProduct> getTopProducts(TopProductsQuery query);

    record TopProductsQuery(UUID storeId, String metric, UUID categoryId, int limit,
                            OffsetDateTime from, OffsetDateTime to) {}
}
