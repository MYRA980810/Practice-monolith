package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.application.port.out.ProcessedWebhookEventPort;
import com.livecomerce.payment.domain.ProcessedWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ProcessedWebhookEventPersistenceAdapter implements ProcessedWebhookEventPort {

    private final ProcessedWebhookEventJpaRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkProcessed(String stripeEventId, String type) {
        try {
            // saveAndFlush (not save): with a client-assigned UUID id, plain save() only calls
            // entityManager.persist(), and Hibernate defers the actual INSERT until the next
            // flush/commit. Catching DataIntegrityViolationException right after a plain save()
            // would never trigger here — the unique-constraint violation would instead surface
            // later, at commit time, as an unhandled exception. Flushing forces the INSERT (and
            // the constraint check) inside this try/catch.
            //
            // REQUIRES_NEW: this insert runs in its own transaction, isolated from the caller's
            // (HandleStripeWebhookService.handle is @Transactional). A failed flush leaves the
            // Hibernate session invalid for the rest of that transaction, so without REQUIRES_NEW
            // a duplicate webhook delivery could poison the whole business-logic transaction even
            // though it never touches this repository again.
            repository.saveAndFlush(ProcessedWebhookEvent.create(stripeEventId, type));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
