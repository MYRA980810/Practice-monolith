package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerNamesAdapterTest {

    @Mock LoadUserPort loadUserPort;

    @InjectMocks SellerNamesAdapter adapter;

    @Test
    void loadNames_whenUserHasAlias_usesAlias() {
        var user = User.create("user@test.com", "hash", "Jane", "Doe", null, Role.SELLER);
        user.updateAlias("jane-doe");
        when(loadUserPort.loadByIds(Set.of(user.getId()))).thenReturn(List.of(user));

        var result = adapter.loadNames(Set.of(user.getId()));

        assertThat(result).containsEntry(user.getId(), "jane-doe");
    }

    @Test
    void loadNames_whenUserHasNoAlias_fallsBackToFullName() {
        var user = User.create("user@test.com", "hash", "Jane", "Doe", null, Role.SELLER);
        when(loadUserPort.loadByIds(Set.of(user.getId()))).thenReturn(List.of(user));

        var result = adapter.loadNames(Set.of(user.getId()));

        assertThat(result).containsEntry(user.getId(), "Jane Doe");
    }

    @Test
    void loadNames_whenUserNotFound_omitsItFromTheMap() {
        var missingId = UUID.randomUUID();
        when(loadUserPort.loadByIds(Set.of(missingId))).thenReturn(List.of());

        var result = adapter.loadNames(Set.of(missingId));

        assertThat(result).isEmpty();
    }
}
