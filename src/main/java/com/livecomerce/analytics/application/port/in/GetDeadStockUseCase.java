package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.DeadStockItem;
import com.livecomerce.analytics.domain.StockAlert;

import java.util.List;
import java.util.UUID;

public interface GetDeadStockUseCase {

    List<DeadStockItem> getDeadStock(UUID storeId, int notSoldInDays);

    List<StockAlert> getStockAlerts(UUID storeId);
}
