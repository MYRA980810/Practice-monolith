package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.SellerIvsChannelPort;
import com.livecomerce.live.domain.SellerIvsChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SellerIvsChannelPersistenceAdapter implements SellerIvsChannelPort {

    private final SellerIvsChannelJpaRepository repository;

    @Override
    public Optional<SellerIvsChannel> loadBySellerId(UUID sellerId) {
        return repository.findBySellerId(sellerId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean trySave(SellerIvsChannel channel) {
        try {
            // saveAndFlush (not save): with a client-assigned UUID id, plain save() only calls
            // entityManager.persist(), and Hibernate defers the actual INSERT until the next
            // flush/commit. Catching DataIntegrityViolationException right after a plain save()
            // would never trigger here — the unique-constraint violation would instead surface
            // later, at commit time, as an unhandled exception. Flushing forces the INSERT (and
            // the constraint check) inside this try/catch.
            //
            // REQUIRES_NEW: this insert runs in its own transaction, isolated from the caller's
            // (StartLiveService.startLive is @Transactional). A failed flush leaves the Hibernate
            // session invalid for the rest of that transaction, so without REQUIRES_NEW a
            // concurrent channel creation could poison the whole business-logic transaction even
            // though it never touches this repository again.
            repository.saveAndFlush(channel);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
