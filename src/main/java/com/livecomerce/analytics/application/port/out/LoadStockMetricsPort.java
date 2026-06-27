package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.DeadStockItem;
import com.livecomerce.analytics.domain.StockAlert;

import java.util.List;
import java.util.UUID;

public interface LoadStockMetricsPort {

    List<DeadStockItem> loadDeadStock(UUID storeId, int notSoldInDays);

    List<StockAlert> loadStockAlerts(UUID storeId);
}
