package com.livecomerce.store.application;

import com.livecomerce.store.SellerAddressAddedEvent;
import com.livecomerce.store.application.port.in.AddSellerAddressUseCase.AddSellerAddressCommand;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.SellerAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddSellerAddressServiceTest {

    @Mock SellerAddressPort sellerAddressPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AddSellerAddressService sut;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void add_whenLimitExceeded_throwsAndNeverSaves() {
        when(sellerAddressPort.countByUserId(USER_ID)).thenReturn(6);
        var cmd = new AddSellerAddressCommand(
                USER_ID, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", false, null, null
        );

        assertThatThrownBy(() -> sut.add(cmd))
                .isInstanceOf(AddressLimitExceededException.class);

        verify(sellerAddressPort, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void add_withIsDefaultTrue_clearsDefaultThenSavesThenPublishesEvent() {
        when(sellerAddressPort.countByUserId(USER_ID)).thenReturn(0);
        when(sellerAddressPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new AddSellerAddressCommand(
                USER_ID, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", true, null, null
        );

        sut.add(cmd);

        var orderVerifier = inOrder(sellerAddressPort, eventPublisher);
        orderVerifier.verify(sellerAddressPort).clearDefaultForUser(USER_ID);
        var captor = ArgumentCaptor.forClass(SellerAddress.class);
        orderVerifier.verify(sellerAddressPort).save(captor.capture());
        orderVerifier.verify(eventPublisher).publishEvent(new SellerAddressAddedEvent(USER_ID));
        assertThat(captor.getValue().isDefault()).isTrue();
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void add_withCoordinates_persistsLatitudeAndLongitude() {
        when(sellerAddressPort.countByUserId(USER_ID)).thenReturn(0);
        when(sellerAddressPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new AddSellerAddressCommand(
                USER_ID, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", false,
                -34.6037, -58.3816
        );

        var captor = ArgumentCaptor.forClass(SellerAddress.class);
        sut.add(cmd);

        verify(sellerAddressPort).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isEqualTo(-34.6037);
        assertThat(captor.getValue().getLongitude()).isEqualTo(-58.3816);
    }
}
