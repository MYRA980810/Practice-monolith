package com.livecomerce.live.application;

import com.livecomerce.live.BuyerDefaultAddressPort;
import com.livecomerce.live.BuyerDefaultAddressPort.ShippingAddress;
import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.live.application.port.in.ExitLiveUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.order.LiveOrderFinalizePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExitLiveService implements ExitLiveUseCase {

    private final LiveOrderFinalizePort liveOrderFinalizePort;
    private final LoadLivePort loadLivePort;
    private final BuyerDefaultAddressPort buyerDefaultAddressPort;
    private final SellerShippingAddressPort sellerShippingAddressPort;

    @Override
    public ExitLiveResponse exitLive(ExitLiveCommand command) {
        var activeOrder = liveOrderFinalizePort
                .loadActiveOrderForBuyerAndLive(command.buyerId(), command.liveId());

        if (activeOrder.isEmpty()) {
            return new ExitLiveResponse(null, null, null);
        }

        var order = activeOrder.get();

        ShippingAddress buyerSavedAddress = null;
        String shippingAddressString = command.shippingAddress();

        if (shippingAddressString == null) {
            buyerSavedAddress = buyerDefaultAddressPort
                    .loadDefaultAddress(command.buyerId())
                    .orElse(null);
            if (buyerSavedAddress != null) {
                shippingAddressString = formatAddress(buyerSavedAddress);
            }
        }

        var finalized = liveOrderFinalizePort.finalize(
                new LiveOrderFinalizePort.FinalizeCommand(
                        order.getId(), command.buyerId(), shippingAddressString));

        var sellerDispatchAddress = loadLivePort.loadById(command.liveId())
                .flatMap(live -> live.getStoreId() != null
                        ? sellerShippingAddressPort.loadByStoreId(live.getStoreId())
                        : Optional.empty())
                .orElse(null);

        return new ExitLiveResponse(finalized, buyerSavedAddress, sellerDispatchAddress);
    }

    private String formatAddress(ShippingAddress address) {
        var sb = new StringBuilder(address.street());
        if (address.extNumber() != null) sb.append(" ").append(address.extNumber());
        sb.append(", ").append(address.city()).append(", ").append(address.state());
        return sb.toString();
    }
}
