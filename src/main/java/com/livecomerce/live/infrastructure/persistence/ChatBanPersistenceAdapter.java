package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.ChatBanPort;
import com.livecomerce.live.domain.ChatBan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ChatBanPersistenceAdapter implements ChatBanPort {

    private final ChatBanJpaRepository repository;

    /**
     * INSERT ... ON CONFLICT DO NOTHING instead of saveAndFlush + catching the constraint
     * violation: the repository's own transactional proxy marks the shared transaction
     * rollback-only when saveAndFlush throws, so the commit would still fail after the catch.
     * REQUIRES_NEW commits the ban before the caller talks to IVS.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void banIfAbsent(ChatBan ban) {
        repository.insertIfAbsent(ban.getId(), ban.getSellerId(), ban.getUserId(),
                ban.getReason(), ban.getBannedBy(), ban.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBanned(UUID sellerId, UUID userId) {
        return repository.existsBySellerIdAndUserId(sellerId, userId);
    }

    @Override
    @Transactional
    public void delete(UUID sellerId, UUID userId) {
        repository.deleteBySellerIdAndUserId(sellerId, userId);
    }
}
