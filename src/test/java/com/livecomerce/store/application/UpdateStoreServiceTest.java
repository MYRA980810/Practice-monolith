package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateStoreUseCase.UpdateStoreCommand;
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
    void update_updatesProfileFields() {
        var store = buildStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));
        when(saveStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new UpdateStoreCommand(USER_ID, "New Name", "Desc", null);
        sut.update(cmd);

        assertThat(store.getName()).isEqualTo("New Name");
        assertThat(store.getDescription()).isEqualTo("Desc");
    }
}
