package com.livecomerce.review.api;

import com.livecomerce.review.application.IncompleteProductRatingsException;
import com.livecomerce.review.application.OrderNotEligibleForReviewException;
import com.livecomerce.review.application.ReviewAlreadyExistsException;
import com.livecomerce.review.application.ReviewOrderNotFoundException;
import com.livecomerce.review.application.ReviewOrderNotOwnedException;
import com.livecomerce.review.application.port.in.ListProductReviewsUseCase;
import com.livecomerce.review.application.port.in.ListStoreReviewsUseCase;
import com.livecomerce.review.application.port.in.SubmitReviewUseCase;
import com.livecomerce.review.application.query.ReviewView;
import com.livecomerce.review.domain.Review;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = ReviewController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
@Import(ReviewControllerTest.SecurityResolverConfig.class)
class ReviewControllerTest {

    @TestConfiguration
    static class SecurityResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mvc;

    @MockitoBean SubmitReviewUseCase submitReviewUseCase;
    @MockitoBean ListStoreReviewsUseCase listStoreReviewsUseCase;
    @MockitoBean ListProductReviewsUseCase listProductReviewsUseCase;

    private static final UUID BUYER_ID      = UUID.randomUUID();
    private static final UUID STORE_ID      = UUID.randomUUID();
    private static final UUID ORDER_ID      = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID    = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                BUYER_ID, "buyer@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static String validRequestBody() {
        return """
                {
                  "descriptionAccuracyRating": 5,
                  "packagingConditionRating": 4,
                  "deliveryTimelinessRating": 5,
                  "sellerAttentionRating": 5,
                  "comment": "Todo perfecto",
                  "anonymous": false,
                  "photoUrls": [],
                  "productRatings": [{"orderItemId":"%s","rating":5}]
                }
                """.formatted(ORDER_ITEM_ID);
    }

    // --- POST /api/orders/{orderId}/review ---

    @Test
    void submitReview_withValidRequest_returns201() throws Exception {
        var review = Review.create(ORDER_ID, STORE_ID, BUYER_ID, 5, 4, 5, 5, "Todo perfecto", false);
        when(submitReviewUseCase.submitReview(any())).thenReturn(review);

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.storeId").value(STORE_ID.toString()));
    }

    @Test
    void submitReview_whenOrderNotFound_returns404() throws Exception {
        when(submitReviewUseCase.submitReview(any())).thenThrow(new ReviewOrderNotFoundException(ORDER_ID));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitReview_whenNotOwned_returns403() throws Exception {
        when(submitReviewUseCase.submitReview(any())).thenThrow(new ReviewOrderNotOwnedException(ORDER_ID, BUYER_ID));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitReview_whenNotDelivered_returns409() throws Exception {
        when(submitReviewUseCase.submitReview(any())).thenThrow(new OrderNotEligibleForReviewException(ORDER_ID));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void submitReview_whenAlreadyReviewed_returns409() throws Exception {
        when(submitReviewUseCase.submitReview(any())).thenThrow(new ReviewAlreadyExistsException(ORDER_ID));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void submitReview_whenIncompleteProductRatings_returns400() throws Exception {
        when(submitReviewUseCase.submitReview(any())).thenThrow(new IncompleteProductRatingsException(ORDER_ID));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReview_withOutOfRangeStars_returns400() throws Exception {
        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descriptionAccuracyRating": 9,
                                  "packagingConditionRating": 4,
                                  "deliveryTimelinessRating": 5,
                                  "sellerAttentionRating": 5,
                                  "anonymous": false,
                                  "productRatings": [{"orderItemId":"%s","rating":5}]
                                }
                                """.formatted(ORDER_ITEM_ID)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/stores/{storeId}/reviews ---

    @Test
    void listStoreReviews_returns200WithPage() throws Exception {
        var view = new ReviewView(ORDER_ID, STORE_ID, "Comprador verificado", 5, 4, 5, 5,
                new java.math.BigDecimal("4.75"), "Todo perfecto", true, List.of(), List.of(), OffsetDateTime.now());
        when(listStoreReviewsUseCase.listByStore(eq(STORE_ID), any())).thenReturn(new PageImpl<>(List.of(view)));

        mvc.perform(get("/api/stores/{storeId}/reviews", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].buyerDisplayName").value("Comprador verificado"));
    }

    @Test
    void listStoreReviews_neverLeaksOrderId() throws Exception {
        // orderId lets anyone deanonymize a buyer via GET /api/orders/{id}; the public
        // listing must never serialize it, anonymous review or not.
        var view = new ReviewView(ORDER_ID, STORE_ID, "Comprador verificado", 5, 4, 5, 5,
                new java.math.BigDecimal("4.75"), "Todo perfecto", true, List.of(), List.of(), OffsetDateTime.now());
        when(listStoreReviewsUseCase.listByStore(eq(STORE_ID), any())).thenReturn(new PageImpl<>(List.of(view)));

        mvc.perform(get("/api/stores/{storeId}/reviews", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").doesNotExist())
                .andExpect(jsonPath("$.content[0].storeId").value(STORE_ID.toString()));
    }

    // --- GET /api/products/{id}/reviews ---

    @Test
    void listProductReviews_returns200WithPage() throws Exception {
        var view = new ReviewView(ORDER_ID, STORE_ID, "Comprador verificado", 5, 4, 5, 5,
                new java.math.BigDecimal("4.75"), "Todo perfecto", true,
                List.of(), List.of(new ReviewView.ProductRatingInfo(PRODUCT_ID, 5)), OffsetDateTime.now());
        when(listProductReviewsUseCase.listByProduct(eq(PRODUCT_ID), any())).thenReturn(new PageImpl<>(List.of(view)));

        mvc.perform(get("/api/products/{id}/reviews", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].buyerDisplayName").value("Comprador verificado"))
                .andExpect(jsonPath("$.content[0].productRatings[0].productId").value(PRODUCT_ID.toString()));
    }

    @Test
    void listProductReviews_neverLeaksOrderId() throws Exception {
        var view = new ReviewView(ORDER_ID, STORE_ID, "Comprador verificado", 5, 4, 5, 5,
                new java.math.BigDecimal("4.75"), "Todo perfecto", true,
                List.of(), List.of(new ReviewView.ProductRatingInfo(PRODUCT_ID, 5)), OffsetDateTime.now());
        when(listProductReviewsUseCase.listByProduct(eq(PRODUCT_ID), any())).thenReturn(new PageImpl<>(List.of(view)));

        mvc.perform(get("/api/products/{id}/reviews", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").doesNotExist());
    }

    @Test
    void submitReview_withTooManyPhotoUrls_returns400() throws Exception {
        var tooManyPhotos = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> "https://cdn.test/photo" + i + ".jpg")
                .collect(java.util.stream.Collectors.joining("\",\"", "\"", "\""));

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descriptionAccuracyRating": 5,
                                  "packagingConditionRating": 4,
                                  "deliveryTimelinessRating": 5,
                                  "sellerAttentionRating": 5,
                                  "comment": "Todo perfecto",
                                  "anonymous": false,
                                  "photoUrls": [%s],
                                  "productRatings": [{"orderItemId":"%s","rating":5}]
                                }
                                """.formatted(tooManyPhotos, ORDER_ITEM_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReview_withOversizedPhotoUrl_returns400() throws Exception {
        var oversizedUrl = "https://cdn.test/" + "a".repeat(500) + ".jpg";

        mvc.perform(post("/api/orders/{orderId}/review", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descriptionAccuracyRating": 5,
                                  "packagingConditionRating": 4,
                                  "deliveryTimelinessRating": 5,
                                  "sellerAttentionRating": 5,
                                  "comment": "Todo perfecto",
                                  "anonymous": false,
                                  "photoUrls": ["%s"],
                                  "productRatings": [{"orderItemId":"%s","rating":5}]
                                }
                                """.formatted(oversizedUrl, ORDER_ITEM_ID)))
                .andExpect(status().isBadRequest());
    }
}
