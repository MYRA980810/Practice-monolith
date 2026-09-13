package com.livecomerce.review.application.port.out;

import com.livecomerce.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LoadProductReviewsPort {

    Page<Review> findByProductId(UUID productId, Pageable pageable);
}
