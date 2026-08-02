package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.domain.Role;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock LoadUserPort loadUserPort;

    @InjectMocks GetCurrentUserService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private static User buildUser() {
        var user = User.create("user@test.com", "hash", "Jane", "Doe", null, Role.SELLER);
        user.updateAvatar("https://res.cloudinary.com/test/avatar.jpg");
        user.updateAlias("jane-doe");
        return user;
    }

    @Test
    void getCurrentUser_whenUserExists_mapsAllFields() {
        var user = buildUser();
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));

        var result = service.getCurrentUser(USER_ID);

        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.phone()).isEqualTo(user.getPhone());
        assertThat(result.role()).isEqualTo(user.getRole());
        assertThat(result.firstName()).isEqualTo(user.getFirstName());
        assertThat(result.lastName()).isEqualTo(user.getLastName());
        assertThat(result.alias()).isEqualTo(user.getAlias());
        assertThat(result.avatarUrl()).isEqualTo(user.getAvatarUrl());
        assertThat(result.profileComplete()).isEqualTo(user.isProfileComplete());
    }

    @Test
    void getCurrentUser_whenUserNotFound_throwsContactNotFoundException() {
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(USER_ID))
                .isInstanceOf(ContactNotFoundException.class);
    }
}
