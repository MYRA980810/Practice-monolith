package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ReserveStockUseCase;
import com.livecomerce.order.StockReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservationAdapter implements StockReservationPort {

    private final ReserveStockUseCase reserveStockUseCase;

    @Override
    public void reserve(ReserveStockCommand command) {
        reserveStockUseCase.reserve(
                new ReserveStockUseCase.ReserveStockCommand(command.variantId(), command.quantity()));
    }
}
