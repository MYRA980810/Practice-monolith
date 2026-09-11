package com.livecomerce.review.application.port.in;

import com.livecomerce.review.application.query.ReviewView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListStoreReviewsUseCase {

    Page<ReviewView> listByStore(UUID storeId, Pageable pageable);
}
