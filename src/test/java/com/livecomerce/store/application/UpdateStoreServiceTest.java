package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateStoreUseCase.UpdateStoreCommand;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase.ShippingAddressData;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateStoreServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;
    @InjectMocks UpdateStoreService sut;

    private static final UUID USER_ID = UUID.randomUUID();

    private Store buildStore() {
        return Store.create(USER_ID, "Test Store", "test-store", null, null);
    }

    @Test
    void update_withNullShippingAddress_onlyUpdatesProfile() {
        var store = buildStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(saveStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new UpdateStoreCommand(USER_ID, "New Name", "Desc", null, null);
        sut.update(cmd);

        assertThat(store.getName()).isEqualTo("New Name");
        assertThat(store.getShippingStreet()).isNull();
    }

    @Test
    void update_withShippingAddress_updatesShippingFields() {
        var store = buildStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(saveStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var shipping = new ShippingAddressData("Calle", "10", null, "Col", "CDMX", "Ciudad", "01000", "MX");
        var cmd = new UpdateStoreCommand(USER_ID, "Name", "Desc", null, shipping);
        sut.update(cmd);

        assertThat(store.getShippingStreet()).isEqualTo("Calle");
        assertThat(store.getShippingCity()).isEqualTo("CDMX");
        assertThat(store.getShippingCountry()).isEqualTo("MX");
    }
}
