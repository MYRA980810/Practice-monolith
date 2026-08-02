package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListSellerPayoutsServiceTest {

    @Mock SellerConnectAccountPort sellerConnectAccountPort;
    @Mock StripeConnectGatewayPort stripeConnectGatewayPort;

    ListSellerPayoutsService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new ListSellerPayoutsService(sellerConnectAccountPort, stripeConnectGatewayPort);
    }

    @Test
    void listPayouts_whenNoConnectAccountExists_returnsEmptyPage() {
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.empty());

        var result = sut.listPayouts(SELLER_ID, null, 10);

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasMore()).isFalse();
        verify(stripeConnectGatewayPort, never()).listPayouts(any(), any(), anyInt());
    }

    @Test
    void listPayouts_whenConnectAccountExists_mapsItemsAndCursorFromStripe() {
        var account = SellerConnectAccount.create(SELLER_ID, "acct_existing");
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.of(account));

        var arrivalDate = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        when(stripeConnectGatewayPort.listPayouts("acct_existing", "po_5", 25)).thenReturn(
                new StripeConnectGatewayPort.PayoutPage(
                        List.of(new StripeConnectGatewayPort.PayoutItem(
                                "po_10", 1500, "usd", "paid", "standard", arrivalDate)),
                        "po_10",
                        true
                ));

        var result = sut.listPayouts(SELLER_ID, "po_5", 25);

        assertThat(result.items()).hasSize(1);
        var item = result.items().get(0);
        assertThat(item.id()).isEqualTo("po_10");
        assertThat(item.amount()).isEqualTo(1500);
        assertThat(item.currency()).isEqualTo("usd");
        assertThat(item.status()).isEqualTo("paid");
        assertThat(item.method()).isEqualTo("standard");
        assertThat(item.arrivalDate()).isEqualTo(arrivalDate);
        assertThat(result.nextCursor()).isEqualTo("po_10");
        assertThat(result.hasMore()).isTrue();
    }
}
