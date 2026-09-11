package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Batched, read-time buyer display-name lookup against the shared {@code
 * users} table. No {@code auth.domain} import — native query bypasses the
 * JpaRepository entity type, reusing {@link Review} purely as the generic
 * carrier (same established smell as analytics' BuyerNameRepository).
 * Named distinctly from analytics' own copy — Spring Data registers JPA
 * repository beans by simple class name, and two {@code BuyerNameRepository}
 * interfaces in different packages collide at context startup.
 */
interface ReviewBuyerNameRepository extends JpaRepository<Review, UUID> {

    @Query(value = "SELECT id, first_name, last_name FROM users WHERE id IN (:buyerIds)", nativeQuery = true)
    List<Object[]> findNamesByIds(@Param("buyerIds") Collection<UUID> buyerIds);
}
