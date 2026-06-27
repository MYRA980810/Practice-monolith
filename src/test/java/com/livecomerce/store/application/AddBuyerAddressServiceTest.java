package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.AddBuyerAddressUseCase.AddBuyerAddressCommand;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddBuyerAddressServiceTest {

    @Mock BuyerAddressPort buyerAddressPort;
    @InjectMocks AddBuyerAddressService sut;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void add_withIsDefaultFalse_savesWithoutClearingDefault() {
        when(buyerAddressPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new AddBuyerAddressCommand(
                USER_ID, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", false
        );

        sut.add(cmd);

        verify(buyerAddressPort, never()).clearDefaultForUser(any());
        var captor = ArgumentCaptor.forClass(BuyerAddress.class);
        verify(buyerAddressPort).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isFalse();
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void add_withIsDefaultTrue_clearsDefaultFirstThenSavesAsDefault() {
        when(buyerAddressPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new AddBuyerAddressCommand(
                USER_ID, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", true
        );

        sut.add(cmd);

        var orderVerifier = inOrder(buyerAddressPort);
        orderVerifier.verify(buyerAddressPort).clearDefaultForUser(USER_ID);
        var captor = ArgumentCaptor.forClass(BuyerAddress.class);
        orderVerifier.verify(buyerAddressPort).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isTrue();
    }
}
