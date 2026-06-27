package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetDeadStockUseCase;
import com.livecomerce.analytics.application.port.out.LoadStockMetricsPort;
import com.livecomerce.analytics.domain.DeadStockItem;
import com.livecomerce.analytics.domain.StockAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetDeadStockService implements GetDeadStockUseCase {

    private final LoadStockMetricsPort loadStockMetricsPort;

    @Override
    public List<DeadStockItem> getDeadStock(UUID storeId, int notSoldInDays) {
        return loadStockMetricsPort.loadDeadStock(storeId, notSoldInDays);
    }

    @Override
    public List<StockAlert> getStockAlerts(UUID storeId) {
        return loadStockMetricsPort.loadStockAlerts(storeId);
    }
}
