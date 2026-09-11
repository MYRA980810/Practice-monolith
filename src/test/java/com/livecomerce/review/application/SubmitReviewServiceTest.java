package com.livecomerce.review.application;

import com.livecomerce.order.LoadOrderForReviewPort;
import com.livecomerce.order.LoadOrderForReviewPort.OrderForReview;
import com.livecomerce.order.LoadOrderForReviewPort.OrderLineForReview;
import com.livecomerce.review.ReviewSubmittedEvent;
import com.livecomerce.review.application.port.in.SubmitReviewUseCase.ProductRatingInput;
import com.livecomerce.review.application.port.in.SubmitReviewUseCase.SubmitReviewCommand;
import com.livecomerce.review.application.port.out.ReviewPersistencePort;
import com.livecomerce.review.domain.Review;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitReviewServiceTest {

    @Mock LoadOrderForReviewPort loadOrderForReviewPort;
    @Mock ReviewPersistencePort reviewPersistencePort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SubmitReviewService service;

    private static final UUID ORDER_ID      = UUID.randomUUID();
    private static final UUID BUYER_ID      = UUID.randomUUID();
    private static final UUID STORE_ID      = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID    = UUID.randomUUID();

    private static OrderForReview deliveredOrder() {
        return new OrderForReview(ORDER_ID, BUYER_ID, STORE_ID, true,
                List.of(new OrderLineForReview(ORDER_ITEM_ID, PRODUCT_ID)));
    }

    private static SubmitReviewCommand validCommand() {
        return new SubmitReviewCommand(
                ORDER_ID, BUYER_ID,
                5, 4, 3, 5,
                "Todo perfecto", false,
                List.of("https://cdn.test/photo1.jpg"),
                List.of(new ProductRatingInput(ORDER_ITEM_ID, 5)));
    }

    @Test
    void submitReview_whenEligible_savesReviewWithComputedRankingImpactScore() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(reviewPersistencePort.trySave(any())).thenReturn(true);

        var result = service.submitReview(validCommand());

        // 0.30*5 + 0.20*4 + 0.25*3 + 0.25*5 = 1.5 + 0.8 + 0.75 + 1.25 = 4.30
        assertThat(result.getRankingImpactScore()).isEqualByComparingTo(new BigDecimal("4.30"));
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getPhotos()).hasSize(1);
        assertThat(result.getProductRatings()).hasSize(1);
        assertThat(result.getProductRatings().getFirst().getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @SuppressWarnings("null")
    void submitReview_publishesReviewSubmittedEvent() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(reviewPersistencePort.trySave(any())).thenReturn(true);

        service.submitReview(validCommand());

        var captor = ArgumentCaptor.forClass(ReviewSubmittedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().buyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    void submitReview_whenOrderNotFound_throwsReviewOrderNotFoundException() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitReview(validCommand()))
                .isInstanceOf(ReviewOrderNotFoundException.class);

        verify(reviewPersistencePort, never()).trySave(any());
    }

    @Test
    void submitReview_whenBuyerMismatch_throwsReviewOrderNotOwnedException() {
        var otherBuyer = UUID.randomUUID();
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));

        var command = new SubmitReviewCommand(ORDER_ID, otherBuyer, 5, 5, 5, 5, null, false, List.of(),
                List.of(new ProductRatingInput(ORDER_ITEM_ID, 5)));

        assertThatThrownBy(() -> service.submitReview(command))
                .isInstanceOf(ReviewOrderNotOwnedException.class);
    }

    @Test
    void submitReview_whenOrderNotDelivered_throwsOrderNotEligibleForReviewException() {
        var notDelivered = new OrderForReview(ORDER_ID, BUYER_ID, STORE_ID, false,
                List.of(new OrderLineForReview(ORDER_ITEM_ID, PRODUCT_ID)));
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(notDelivered));

        assertThatThrownBy(() -> service.submitReview(validCommand()))
                .isInstanceOf(OrderNotEligibleForReviewException.class);
    }

    @Test
    void submitReview_whenAlreadyReviewed_throwsReviewAlreadyExistsException() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.submitReview(validCommand()))
                .isInstanceOf(ReviewAlreadyExistsException.class);

        verify(reviewPersistencePort, never()).trySave(any());
    }

    @Test
    void submitReview_whenConcurrentDoubleSubmitRacesThePreCheck_throwsReviewAlreadyExistsException() {
        // Simulates the TOCTOU window: existsByOrderId() sees no row yet (fast-path passes),
        // but a concurrent request already won the insert by the time this one flushes.
        // trySave() must be the actual guarantee, translating the DB-level conflict into 409.
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(reviewPersistencePort.trySave(any())).thenReturn(false);

        assertThatThrownBy(() -> service.submitReview(validCommand()))
                .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    void submitReview_whenProductRatingsIncomplete_throwsIncompleteProductRatingsException() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(false);

        var command = new SubmitReviewCommand(ORDER_ID, BUYER_ID, 5, 5, 5, 5, null, false, List.of(),
                List.of()); // missing the rating for ORDER_ITEM_ID

        assertThatThrownBy(() -> service.submitReview(command))
                .isInstanceOf(IncompleteProductRatingsException.class);

        verify(reviewPersistencePort, never()).trySave(any());
    }

    @Test
    void submitReview_whenProductRatingsCoverUnknownItem_throwsIncompleteProductRatingsException() {
        when(loadOrderForReviewPort.loadForReview(ORDER_ID)).thenReturn(Optional.of(deliveredOrder()));
        when(reviewPersistencePort.existsByOrderId(ORDER_ID)).thenReturn(false);

        var command = new SubmitReviewCommand(ORDER_ID, BUYER_ID, 5, 5, 5, 5, null, false, List.of(),
                List.of(new ProductRatingInput(UUID.randomUUID(), 5))); // wrong item id

        assertThatThrownBy(() -> service.submitReview(command))
                .isInstanceOf(IncompleteProductRatingsException.class);
    }
}
