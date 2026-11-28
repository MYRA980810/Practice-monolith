package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.SellerShippingAddressPort.ShippingAddress;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.AddressType;
import com.livecomerce.store.domain.SellerAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerShippingAddressAdapterTest {

    @Mock SellerAddressPort sellerAddressPort;

    @InjectMocks SellerShippingAddressAdapter adapter;

    private static final UUID SELLER_ID = UUID.randomUUID();

    @Test
    void loadBySellerId_mapsTheSellersDefaultAddress() {
        var address = SellerAddress.create(SELLER_ID, "Calle Falsa", "123", "4B", "Centro",
                "CDMX", "Ciudad de Mexico", "01000", "MX", null, null, AddressType.STORE);
        when(sellerAddressPort.loadDefaultByUserId(SELLER_ID)).thenReturn(Optional.of(address));

        var result = adapter.loadBySellerId(SELLER_ID);

        assertThat(result).contains(new ShippingAddress(
                "Calle Falsa", "123", "4B", "Centro", "CDMX", "Ciudad de Mexico", "01000", "MX"));
    }

    @Test
    void loadBySellerId_withoutDefaultAddress_returnsEmpty() {
        when(sellerAddressPort.loadDefaultByUserId(SELLER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.loadBySellerId(SELLER_ID)).isEmpty();
    }
}
