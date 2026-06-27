package com.livecomerce.live.application.port.in;

import com.livecomerce.live.BuyerDefaultAddressPort;
import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.order.domain.Order;
import jakarta.annotation.Nullable;

import java.util.UUID;

public interface ExitLiveUseCase {

    ExitLiveResponse exitLive(ExitLiveCommand command);

    record ExitLiveCommand(
            UUID liveId,
            UUID buyerId,
            @Nullable String shippingAddress,
            @Nullable String stripePaymentMethodId
    ) {}

    record ExitLiveResponse(
            @Nullable Order order,
            @Nullable BuyerDefaultAddressPort.ShippingAddress buyerAddress,
            @Nullable SellerShippingAddressPort.ShippingAddress sellerDispatchAddress
    ) {}
}
