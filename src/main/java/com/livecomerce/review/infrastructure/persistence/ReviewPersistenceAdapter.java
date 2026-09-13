package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.review.application.port.out.LoadProductReviewsPort;
import com.livecomerce.review.application.port.out.ReviewPersistencePort;
import com.livecomerce.review.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ReviewPersistenceAdapter implements ReviewPersistencePort, LoadProductReviewsPort {

    private final ReviewJpaRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean trySave(Review review) {
        try {
            // saveAndFlush (not save): with a client-assigned UUID id (order_id), plain save()
            // only calls entityManager.persist(), and Hibernate defers the actual INSERT until
            // the next flush/commit. Catching DataIntegrityViolationException right after a
            // plain save() would never trigger here — the unique-constraint violation would
            // instead surface later, at commit time, as an unhandled exception. Flushing forces
            // the INSERT (and the constraint check) inside this try/catch.
            //
            // REQUIRES_NEW: this insert runs in its own transaction, isolated from the caller's
            // (SubmitReviewService.submitReview is @Transactional). A failed flush leaves the
            // Hibernate session invalid for the rest of that transaction, so without REQUIRES_NEW
            // a concurrent double-submit could poison the whole business-logic transaction even
            // though it never touches this repository again.
            repository.saveAndFlush(review);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return repository.existsById(orderId);
    }

    @Override
    public Page<Review> findByStoreId(UUID storeId, Pageable pageable) {
        return repository.findByStoreId(storeId, pageable);
    }

    @Override
    public Page<Review> findByProductId(UUID productId, Pageable pageable) {
        return repository.findByProductId(productId, pageable);
    }
}
