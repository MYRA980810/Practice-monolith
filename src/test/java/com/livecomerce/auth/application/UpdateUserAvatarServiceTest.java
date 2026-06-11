package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.UpdateUserAvatarUseCase.UpdateUserAvatarCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserAvatarServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;

    @InjectMocks UpdateUserAvatarService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String AVATAR_URL = "https://res.cloudinary.com/test/avatar.jpg";

    private static User buildUser() {
        return User.create("user@test.com", "hash", "Jane", "Doe", null, com.livecomerce.auth.domain.Role.SELLER);
    }

    @Test
    void updateAvatar_whenUserExists_updatesAndReturnsUrl() {
        var user = buildUser();
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateAvatar(new UpdateUserAvatarCommand(USER_ID, AVATAR_URL));

        assertThat(result).isEqualTo(AVATAR_URL);
        assertThat(user.getAvatarUrl()).isEqualTo(AVATAR_URL);
        verify(saveUserPort).save(user);
    }

    @Test
    void updateAvatar_whenUserNotFound_throwsContactNotFoundException() {
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateAvatar(new UpdateUserAvatarCommand(USER_ID, AVATAR_URL)))
                .isInstanceOf(ContactNotFoundException.class);
    }
}
