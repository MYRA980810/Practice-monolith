package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.TopProduct;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoadProductMetricsPort {

    List<TopProduct> loadTopProducts(UUID storeId, String metric, UUID categoryId, int limit,
                                     OffsetDateTime from, OffsetDateTime to);
}
