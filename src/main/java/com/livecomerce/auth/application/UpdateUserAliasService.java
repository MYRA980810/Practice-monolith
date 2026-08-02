package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.UpdateUserAliasUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserAliasService implements UpdateUserAliasUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @Override
    public String updateAlias(UpdateUserAliasCommand command) {
        var user = loadUserPort.loadById(command.userId())
                .orElseThrow(ContactNotFoundException::new);
        loadUserPort.loadByAlias(command.alias())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new AliasAlreadyTakenException(command.alias());
                });
        user.updateAlias(command.alias());
        var saved = saveUserPort.save(user);
        return saved.getAlias();
    }
}
