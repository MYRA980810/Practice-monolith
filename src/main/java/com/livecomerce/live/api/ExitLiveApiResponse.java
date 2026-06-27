package com.livecomerce.live.api;

import com.livecomerce.live.BuyerDefaultAddressPort;
import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.live.application.port.in.ExitLiveUseCase.ExitLiveResponse;
import com.livecomerce.order.api.OrderResponse;
import jakarta.annotation.Nullable;

public record ExitLiveApiResponse(
        @Nullable OrderResponse order,
        @Nullable BuyerDefaultAddressPort.ShippingAddress buyerAddress,
        @Nullable SellerShippingAddressPort.ShippingAddress sellerDispatchAddress
) {
    public static ExitLiveApiResponse from(ExitLiveResponse response) {
        return new ExitLiveApiResponse(
                response.order() != null ? OrderResponse.from(response.order()) : null,
                response.buyerAddress(),
                response.sellerDispatchAddress()
        );
    }
}
