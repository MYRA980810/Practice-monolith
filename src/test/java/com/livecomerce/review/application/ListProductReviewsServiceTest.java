package com.livecomerce.review.application;

import com.livecomerce.review.application.port.out.LoadBuyerNamesPort;
import com.livecomerce.review.application.port.out.LoadProductReviewsPort;
import com.livecomerce.review.domain.Review;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProductReviewsServiceTest {

    @Mock LoadProductReviewsPort loadProductReviewsPort;
    @Mock LoadBuyerNamesPort loadBuyerNamesPort;

    @InjectMocks ListProductReviewsService service;

    private static final UUID STORE_ID      = UUID.randomUUID();
    private static final UUID BUYER_ID      = UUID.randomUUID();
    private static final UUID ORDER_ID      = UUID.randomUUID();
    private static final UUID PRODUCT_ID       = UUID.randomUUID();
    private static final UUID OTHER_PRODUCT_ID = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID    = UUID.randomUUID();
    private static final UUID OTHER_ORDER_ITEM_ID = UUID.randomUUID();

    private static Review buildReview(boolean anonymous) {
        var review = Review.create(ORDER_ID, STORE_ID, BUYER_ID, 5, 5, 5, 5, "Excelente", anonymous);
        review.addProductRating(ORDER_ITEM_ID, PRODUCT_ID, 5);
        return review;
    }

    private static Review buildMultiProductReview() {
        var review = Review.create(ORDER_ID, STORE_ID, BUYER_ID, 5, 5, 5, 5, "Excelente", false);
        review.addProductRating(ORDER_ITEM_ID, PRODUCT_ID, 5);
        review.addProductRating(OTHER_ORDER_ITEM_ID, OTHER_PRODUCT_ID, 2);
        return review;
    }

    @Test
    void listByProduct_whenNotAnonymous_showsResolvedBuyerName() {
        var review = buildReview(false);
        var page = new PageImpl<>(List.of(review));
        when(loadProductReviewsPort.findByProductId(eq(PRODUCT_ID), any())).thenReturn(page);
        when(loadBuyerNamesPort.loadNames(Set.of(BUYER_ID))).thenReturn(Map.of(BUYER_ID, "Ana López"));

        var result = service.listByProduct(PRODUCT_ID, PageRequest.of(0, 20));

        assertThat(result.getContent().getFirst().buyerDisplayName()).isEqualTo("Ana López");
        assertThat(result.getContent().getFirst().storeId()).isEqualTo(STORE_ID);
    }

    @Test
    void listByProduct_whenAnonymous_masksBuyerNameAndSkipsLookup() {
        var review = buildReview(true);
        var page = new PageImpl<>(List.of(review));
        when(loadProductReviewsPort.findByProductId(eq(PRODUCT_ID), any())).thenReturn(page);

        var result = service.listByProduct(PRODUCT_ID, PageRequest.of(0, 20));

        assertThat(result.getContent().getFirst().buyerDisplayName()).isEqualTo("Comprador verificado");
        verify(loadBuyerNamesPort, never()).loadNames(any());
    }

    @Test
    void listByProduct_whenReviewCoversMultipleProducts_onlyShowsRequestedProductsRating() {
        var review = buildMultiProductReview();
        var page = new PageImpl<>(List.of(review));
        when(loadProductReviewsPort.findByProductId(any(), any())).thenReturn(page);
        when(loadBuyerNamesPort.loadNames(Set.of(BUYER_ID))).thenReturn(Map.of(BUYER_ID, "Ana López"));

        var resultForProduct = service.listByProduct(PRODUCT_ID, PageRequest.of(0, 20));
        var resultForOtherProduct = service.listByProduct(OTHER_PRODUCT_ID, PageRequest.of(0, 20));

        assertThat(resultForProduct.getContent().getFirst().productRatings())
                .extracting("productId")
                .containsExactly(PRODUCT_ID);
        assertThat(resultForOtherProduct.getContent().getFirst().productRatings())
                .extracting("productId")
                .containsExactly(OTHER_PRODUCT_ID);
    }
}
