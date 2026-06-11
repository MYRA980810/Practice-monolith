package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.UpdateUserAvatarUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserAvatarService implements UpdateUserAvatarUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @Override
    public String updateAvatar(UpdateUserAvatarCommand command) {
        var user = loadUserPort.loadById(command.userId())
                .orElseThrow(ContactNotFoundException::new);
        user.updateAvatar(command.avatarUrl());
        var saved = saveUserPort.save(user);
        return saved.getAvatarUrl();
    }
}
