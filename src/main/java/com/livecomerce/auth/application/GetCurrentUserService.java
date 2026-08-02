package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.GetCurrentUserUseCase;
import com.livecomerce.auth.application.port.in.UserProfileResult;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public UserProfileResult getCurrentUser(UUID userId) {
        var user = loadUserPort.loadById(userId)
                .orElseThrow(ContactNotFoundException::new);
        return new UserProfileResult(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getAlias(),
                user.getAvatarUrl(),
                user.isProfileComplete()
        );
    }
}
