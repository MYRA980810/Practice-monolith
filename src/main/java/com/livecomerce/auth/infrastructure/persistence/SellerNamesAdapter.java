package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.domain.User;
import com.livecomerce.live.LoadSellerNamesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
class SellerNamesAdapter implements LoadSellerNamesPort {

    private final LoadUserPort loadUserPort;

    @Override
    public Map<UUID, String> loadNames(Collection<UUID> sellerIds) {
        // Users without any usable name are omitted, like unknown ids: callers apply their own fallback.
        return loadUserPort.loadByIds(sellerIds).stream()
                .filter(user -> !displayName(user).isEmpty())
                .collect(Collectors.toMap(User::getId, SellerNamesAdapter::displayName));
    }

    /** Alias if set, else the non-blank parts of first + last name; empty when there is nothing. */
    private static String displayName(User user) {
        if (user.getAlias() != null && !user.getAlias().isBlank()) {
            return user.getAlias();
        }
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
