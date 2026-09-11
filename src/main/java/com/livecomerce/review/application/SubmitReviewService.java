package com.livecomerce.review.application;

import com.livecomerce.order.LoadOrderForReviewPort;
import com.livecomerce.review.ReviewSubmittedEvent;
import com.livecomerce.review.application.port.in.SubmitReviewUseCase;
import com.livecomerce.review.application.port.out.ReviewPersistencePort;
import com.livecomerce.review.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmitReviewService implements SubmitReviewUseCase {

    private final LoadOrderForReviewPort loadOrderForReviewPort;
    private final ReviewPersistencePort reviewPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Review submitReview(SubmitReviewCommand command) {
        var order = loadOrderForReviewPort.loadForReview(command.orderId())
                .orElseThrow(() -> new ReviewOrderNotFoundException(command.orderId()));

        if (!order.buyerId().equals(command.buyerId())) {
            throw new ReviewOrderNotOwnedException(command.orderId(), command.buyerId());
        }

        if (!order.delivered()) {
            throw new OrderNotEligibleForReviewException(command.orderId());
        }

        if (reviewPersistencePort.existsByOrderId(command.orderId())) {
            throw new ReviewAlreadyExistsException(command.orderId());
        }

        var expectedItemIds = order.items().stream()
                .map(LoadOrderForReviewPort.OrderLineForReview::orderItemId)
                .collect(Collectors.toSet());
        var submittedItemIds = command.productRatings().stream()
                .map(ProductRatingInput::orderItemId)
                .collect(Collectors.toSet());
        if (!expectedItemIds.equals(submittedItemIds)) {
            throw new IncompleteProductRatingsException(command.orderId());
        }

        var review = Review.create(
                command.orderId(), order.storeId(), command.buyerId(),
                command.descriptionAccuracyRating(), command.packagingConditionRating(),
                command.deliveryTimelinessRating(), command.sellerAttentionRating(),
                command.comment(), command.anonymous());

        if (command.photoUrls() != null) {
            int position = 0;
            for (var url : command.photoUrls()) {
                review.addPhoto(url, position++);
            }
        }

        var productIdByItemId = order.items().stream()
                .collect(Collectors.toMap(
                        LoadOrderForReviewPort.OrderLineForReview::orderItemId,
                        LoadOrderForReviewPort.OrderLineForReview::productId));
        for (var rating : command.productRatings()) {
            var productId = productIdByItemId.get(rating.orderItemId());
            review.addProductRating(rating.orderItemId(), productId, rating.rating());
        }

        // existsByOrderId() above is a fast-path only; it cannot prevent a concurrent
        // double-submit by itself (TOCTOU). trySave() is the actual correctness guarantee:
        // it attempts the insert and translates a DB-level unique-constraint violation on the
        // client-assigned orderId into ReviewAlreadyExistsException.
        if (!reviewPersistencePort.trySave(review)) {
            throw new ReviewAlreadyExistsException(command.orderId());
        }

        eventPublisher.publishEvent(new ReviewSubmittedEvent(review.getOrderId(), review.getStoreId(), review.getBuyerId()));

        return review;
    }
}
