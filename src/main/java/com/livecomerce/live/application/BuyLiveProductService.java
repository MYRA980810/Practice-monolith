package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.BuyLiveProductUseCase;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.AtomicLiveProductStockPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.domain.*;
import com.livecomerce.order.LivePurchasePort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BuyLiveProductService implements BuyLiveProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuyLiveProductService.class);

    private final LoadLiveProductPort        loadLiveProductPort;
    private final LoadLivePort               loadLivePort;
    private final AtomicLiveProductStockPort atomicStockPort;
    private final LivePurchasePort           livePurchasePort;
    private final AgoraRtmMessagePort        agoraRtmMessagePort;
    private final ObjectMapper               objectMapper;

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

        var updated = atomicStockPort.atomicIncrementStockSold(lp.getId(), command.quantity());
        if (updated.isEmpty()) {
            throw new LiveProductOutOfStockException(lp.getId());
        }

        var itemType = lp.isHot() ? OrderItemType.HOT_PRODUCT : OrderItemType.PRODUCT;

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

            int stockRemaining = Math.max(0, lp.getStockAllocated() - (lp.getStockSold() + command.quantity()));
            try {
                String stockPayload = objectMapper.writeValueAsString(Map.of(
                        "type",           "stock-update",
                        "liveProductId",  lp.getId(),
                        "stockRemaining", stockRemaining
                ));
                agoraRtmMessagePort.sendChannelMessage("live-chat:" + live.getId(), stockPayload);
            } catch (Exception e) {
                log.warn("Agora RTM stock-update failed: {}", e.getMessage());
            }

            try {
                String orderPayload = objectMapper.writeValueAsString(Map.of(
                        "type",          "order-confirmed",
                        "liveProductId", lp.getId(),
                        "productName",   lp.getProductNameSnapshot(),
                        "quantity",      command.quantity(),
                        "totalAmount",   lp.getPriceSnapshot().multiply(BigDecimal.valueOf(command.quantity()))
                ));
                agoraRtmMessagePort.sendPeerMessage(live.getSellerId().toString(), orderPayload);
            } catch (Exception e) {
                log.warn("Agora RTM order-confirmed to seller failed: {}", e.getMessage());
            }

            return order;
        } catch (LiveProductOutOfStockException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            atomicStockPort.decrementStockSold(lp.getId(), command.quantity());
            throw e;
        }
    }
}
