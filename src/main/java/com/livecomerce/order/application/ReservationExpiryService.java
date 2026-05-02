package com.livecomerce.order.application;

import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ReservationExpiryService {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;

    @Transactional
    public void expireOverdueReservations() {
        var now = OffsetDateTime.now();
        loadOrderPort.loadOrdersWithExpiredReservations()
                .forEach(order -> {
                    order.expireReservedItems(now);
                    saveOrderPort.save(order);
                });
    }
}
