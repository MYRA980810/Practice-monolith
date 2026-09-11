package com.livecomerce.review.application.port.out;

import com.livecomerce.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewPersistencePort {

    boolean trySave(Review review);

    boolean existsByOrderId(UUID orderId);

    Page<Review> findByStoreId(UUID storeId, Pageable pageable);
}
