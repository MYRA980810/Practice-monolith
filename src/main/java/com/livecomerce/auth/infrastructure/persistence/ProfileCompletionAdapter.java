package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.domain.User;
import com.livecomerce.live.ProfileCompletionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProfileCompletionAdapter implements ProfileCompletionPort {

    private final LoadUserPort loadUserPort;

    @Override
    public boolean isProfileComplete(UUID userId) {
        return loadUserPort.loadById(userId).map(User::isProfileComplete).orElse(false);
    }
}
