package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.UpdateUserAliasUseCase.UpdateUserAliasCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserAliasServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;

    @InjectMocks UpdateUserAliasService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ALIAS = "jane-doe";

    private static User buildUser() {
        return User.create("user@test.com", "hash", "Jane", "Doe", null, com.livecomerce.auth.domain.Role.SELLER);
    }

    @Test
    void updateAlias_whenUserExistsAndAliasFree_updatesAndReturnsAlias() {
        var user = buildUser();
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(loadUserPort.loadByAlias(ALIAS)).thenReturn(Optional.empty());
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateAlias(new UpdateUserAliasCommand(USER_ID, ALIAS));

        assertThat(result).isEqualTo(ALIAS);
        assertThat(user.getAlias()).isEqualTo(ALIAS);
        verify(saveUserPort).save(user);
    }

    @Test
    void updateAlias_whenUserNotFound_throwsContactNotFoundException() {
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateAlias(new UpdateUserAliasCommand(USER_ID, ALIAS)))
                .isInstanceOf(ContactNotFoundException.class);
    }

    @Test
    void updateAlias_whenAliasTakenByAnotherUser_throwsAliasAlreadyTakenException() {
        var user = buildUser();
        var otherUser = buildUser();
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(loadUserPort.loadByAlias(ALIAS)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() ->
                service.updateAlias(new UpdateUserAliasCommand(USER_ID, ALIAS)))
                .isInstanceOf(AliasAlreadyTakenException.class);
    }

    @Test
    void updateAlias_whenUserResetsOwnCurrentAlias_doesNotThrow() {
        var user = buildUser();
        user.updateAlias(ALIAS);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(loadUserPort.loadByAlias(ALIAS)).thenReturn(Optional.of(user));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateAlias(new UpdateUserAliasCommand(USER_ID, ALIAS));

        assertThat(result).isEqualTo(ALIAS);
    }
}
