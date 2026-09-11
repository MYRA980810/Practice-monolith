package com.livecomerce.review.application;

import com.livecomerce.review.application.port.out.LoadBuyerNamesPort;
import com.livecomerce.review.application.port.out.ReviewPersistencePort;
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
class ListStoreReviewsServiceTest {

    @Mock ReviewPersistencePort reviewPersistencePort;
    @Mock LoadBuyerNamesPort loadBuyerNamesPort;

    @InjectMocks ListStoreReviewsService service;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private static Review buildReview(boolean anonymous) {
        return Review.create(ORDER_ID, STORE_ID, BUYER_ID, 5, 5, 5, 5, "Excelente", anonymous);
    }

    @Test
    void listByStore_whenNotAnonymous_showsResolvedBuyerName() {
        var review = buildReview(false);
        var page = new PageImpl<>(List.of(review));
        when(reviewPersistencePort.findByStoreId(eq(STORE_ID), any())).thenReturn(page);
        when(loadBuyerNamesPort.loadNames(Set.of(BUYER_ID))).thenReturn(Map.of(BUYER_ID, "Ana López"));

        var result = service.listByStore(STORE_ID, PageRequest.of(0, 20));

        assertThat(result.getContent().getFirst().buyerDisplayName()).isEqualTo("Ana López");
    }

    @Test
    void listByStore_whenAnonymous_masksBuyerNameAndSkipsLookup() {
        var review = buildReview(true);
        var page = new PageImpl<>(List.of(review));
        when(reviewPersistencePort.findByStoreId(eq(STORE_ID), any())).thenReturn(page);

        var result = service.listByStore(STORE_ID, PageRequest.of(0, 20));

        assertThat(result.getContent().getFirst().buyerDisplayName()).isEqualTo("Comprador verificado");
        verify(loadBuyerNamesPort, never()).loadNames(any());
    }
}
