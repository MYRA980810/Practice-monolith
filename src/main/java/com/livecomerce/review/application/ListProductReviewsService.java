package com.livecomerce.review.application;

import com.livecomerce.review.application.port.in.ListProductReviewsUseCase;
import com.livecomerce.review.application.port.out.LoadBuyerNamesPort;
import com.livecomerce.review.application.port.out.LoadProductReviewsPort;
import com.livecomerce.review.application.query.ReviewView;
import com.livecomerce.review.domain.Review;
import com.livecomerce.review.domain.ReviewPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListProductReviewsService implements ListProductReviewsUseCase {

    private static final String ANONYMOUS_LABEL = "Comprador verificado";

    private final LoadProductReviewsPort loadProductReviewsPort;
    private final LoadBuyerNamesPort loadBuyerNamesPort;

    @Override
    public Page<ReviewView> listByProduct(UUID productId, Pageable pageable) {
        var page = loadProductReviewsPort.findByProductId(productId, pageable);

        Set<UUID> buyerIds = page.getContent().stream()
                .filter(r -> !r.isAnonymous())
                .map(Review::getBuyerId)
                .collect(Collectors.toSet());
        var names = buyerIds.isEmpty() ? Map.<UUID, String>of() : loadBuyerNamesPort.loadNames(buyerIds);

        return page.map(review -> toView(review, names, productId));
    }

    private ReviewView toView(Review review, Map<UUID, String> names, UUID productId) {
        var displayName = review.isAnonymous() ? ANONYMOUS_LABEL : names.get(review.getBuyerId());
        var photoUrls = review.getPhotos().stream()
                .sorted(Comparator.comparingInt(ReviewPhoto::getPosition))
                .map(ReviewPhoto::getUrl)
                .toList();
        // This endpoint is scoped to a single product's reviews: a review from a
        // multi-item order carries one ProductRating per product it covers, but
        // only the rating for the requested product should ever be exposed here
        // — otherwise a buyer's rating of a *different* product on the same order
        // leaks into this product's review list.
        var productRatings = review.getProductRatings().stream()
                .filter(pr -> pr.getProductId().equals(productId))
                .map(pr -> new ReviewView.ProductRatingInfo(pr.getProductId(), pr.getRating()))
                .toList();
        return new ReviewView(
                review.getOrderId(),
                review.getStoreId(),
                displayName,
                review.getDescriptionAccuracyRating(),
                review.getPackagingConditionRating(),
                review.getDeliveryTimelinessRating(),
                review.getSellerAttentionRating(),
                review.getRankingImpactScore(),
                review.getComment(),
                review.isAnonymous(),
                photoUrls,
                productRatings,
                review.getCreatedAt()
        );
    }
}
