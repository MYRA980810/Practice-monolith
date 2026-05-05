package com.livecomerce.order.application;

import com.livecomerce.order.OrderItemReleasedEvent;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ReservationExpiryService {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void expireOverdueReservations() {
        var now = OffsetDateTime.now();
        loadOrderPort.loadOrdersWithExpiredReservations()
                .forEach(order -> {
                    var expired = order.expireReservedItems(now);
                    saveOrderPort.save(order);
                    expired.forEach(item -> eventPublisher.publishEvent(
                            new OrderItemReleasedEvent(order.getId(), item.getId(), item.getProductId(), item.getQuantity())));
                });
    }
}
