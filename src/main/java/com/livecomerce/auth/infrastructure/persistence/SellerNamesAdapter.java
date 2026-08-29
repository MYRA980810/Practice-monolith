package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.domain.User;
import com.livecomerce.live.LoadSellerNamesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class SellerNamesAdapter implements LoadSellerNamesPort {

    private final LoadUserPort loadUserPort;

    @Override
    public Map<UUID, String> loadNames(Collection<UUID> sellerIds) {
        return loadUserPort.loadByIds(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, SellerNamesAdapter::displayName));
    }

    private static String displayName(User user) {
        if (user.getAlias() != null && !user.getAlias().isBlank()) {
            return user.getAlias();
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
