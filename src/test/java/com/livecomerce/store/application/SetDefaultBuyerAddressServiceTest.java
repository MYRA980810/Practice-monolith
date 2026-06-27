package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetDefaultBuyerAddressServiceTest {

    @Mock BuyerAddressPort buyerAddressPort;
    @InjectMocks SetDefaultBuyerAddressService sut;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();

    private BuyerAddress addressOwnedBy(UUID ownerId) {
        return BuyerAddress.create(ownerId, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX");
    }

    @Test
    void setDefault_withValidOwnership_clearsAndSetsDefault() {
        var address = addressOwnedBy(USER_ID);
        when(buyerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(buyerAddressPort.save(address)).thenReturn(address);

        sut.setDefault(USER_ID, ADDRESS_ID);

        var orderVerifier = inOrder(buyerAddressPort);
        orderVerifier.verify(buyerAddressPort).clearDefaultForUser(USER_ID);
        orderVerifier.verify(buyerAddressPort).save(address);
        assertThat(address.isDefault()).isTrue();
    }

    @Test
    void setDefault_withWrongOwner_throwsAccessDeniedException() {
        var address = addressOwnedBy(OTHER_USER);
        when(buyerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> sut.setDefault(USER_ID, ADDRESS_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(buyerAddressPort, never()).clearDefaultForUser(any());
        verify(buyerAddressPort, never()).save(any());
    }

    @Test
    void setDefault_whenAddressNotFound_throwsIllegalArgumentException() {
        when(buyerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.setDefault(USER_ID, ADDRESS_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
