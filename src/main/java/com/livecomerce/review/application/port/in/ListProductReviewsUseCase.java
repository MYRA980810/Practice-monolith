package com.livecomerce.review.application.port.in;

import com.livecomerce.review.application.query.ReviewView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListProductReviewsUseCase {

    Page<ReviewView> listByProduct(UUID productId, Pageable pageable);
}
