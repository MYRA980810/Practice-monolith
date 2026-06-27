package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.BuyLiveProductUseCase;
import com.livecomerce.live.application.port.out.AtomicLiveProductStockPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.domain.*;
import com.livecomerce.order.LivePurchasePort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BuyLiveProductService implements BuyLiveProductUseCase {

    private final LoadLiveProductPort        loadLiveProductPort;
    private final LoadLivePort               loadLivePort;
    private final AtomicLiveProductStockPort atomicStockPort;
    private final LivePurchasePort           livePurchasePort;
    private final LiveBroadcastService       broadcastService;

    @Override
    public Order buyLiveProduct(BuyLiveProductCommand command) {
        var lp   = loadLiveProductPort.loadById(command.liveProductId())
                .orElseThrow(() -> new LiveProductNotFoundException(command.liveProductId()));
        var live = loadLivePort.loadById(lp.getLive().getId())
                .orElseThrow(() -> new LiveNotFoundException(lp.getLive().getId()));

        if (live.getStatus() != LiveStatus.LIVE) {
            throw new IllegalStateException(
                    "Cannot buy product: live session is not LIVE, status=" + live.getStatus());
        }

        // Atomic stock check and increment
        var updated = atomicStockPort.atomicIncrementStockSold(lp.getId(), command.quantity());
        if (updated.isEmpty()) {
            throw new LiveProductOutOfStockException(lp.getId());
        }

        // Determine order item type
        var itemType = lp.isHot() ? OrderItemType.HOT_PRODUCT : OrderItemType.PRODUCT;

        // Place order item — if it fails, compensate stock
        try {
            var order = livePurchasePort.placeItem(new LivePurchasePort.LivePurchaseCommand(
                    command.buyerId(),
                    live.getStoreId(),
                    live.getId(),
                    lp.getProductId(),
                    lp.getVariantId(),
                    lp.getProductNameSnapshot(),
                    lp.getPriceSnapshot(),
                    lp.getCurrencySnapshot(),
                    command.quantity(),
                    itemType));

            // Broadcast remaining stock (best-effort domain approximation)
            int stockRemaining = lp.getStockAllocated() - (lp.getStockSold() + command.quantity());
            broadcastService.broadcastStockUpdate(live.getId(), lp.getId(), Math.max(0, stockRemaining));

            return order;
        } catch (Exception e) {
            atomicStockPort.decrementStockSold(lp.getId(), command.quantity());
            throw e;
        }
    }
}
