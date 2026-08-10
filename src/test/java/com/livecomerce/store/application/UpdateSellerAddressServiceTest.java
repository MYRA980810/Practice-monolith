package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateSellerAddressUseCase.UpdateSellerAddressCommand;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.AddressType;
import com.livecomerce.store.domain.SellerAddress;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSellerAddressServiceTest {

    @Mock SellerAddressPort sellerAddressPort;
    @InjectMocks UpdateSellerAddressService sut;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();

    private SellerAddress addressOwnedBy(UUID ownerId) {
        return SellerAddress.create(ownerId, "Calle", "1", null, "Col", "CDMX", "Ciudad", "01000", "MX", null, null,
                AddressType.OTHER);
    }

    private UpdateSellerAddressCommand commandFor(UUID userId) {
        return new UpdateSellerAddressCommand(
                userId, ADDRESS_ID,
                "Av. Nueva", "5678", "Depto 2", "Palermo", "Rosario", "Santa Fe",
                "S2000", "AR", -32.9468, -60.6393, AddressType.STORE
        );
    }

    @Test
    void update_withValidOwnership_mutatesAndSaves() {
        var address = addressOwnedBy(USER_ID);
        when(sellerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(sellerAddressPort.save(address)).thenReturn(address);

        var result = sut.update(commandFor(USER_ID));

        assertThat(result.getStreet()).isEqualTo("Av. Nueva");
        assertThat(result.getExtNumber()).isEqualTo("5678");
        assertThat(result.getCity()).isEqualTo("Rosario");
        assertThat(result.getAddressType()).isEqualTo(AddressType.STORE);
    }

    @Test
    void update_whenAddressNotFound_throwsAddressNotFoundException() {
        when(sellerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(commandFor(USER_ID)))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void update_withWrongOwner_throwsAccessDeniedException() {
        var address = addressOwnedBy(OTHER_USER);
        when(sellerAddressPort.loadById(ADDRESS_ID)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> sut.update(commandFor(USER_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
