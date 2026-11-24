package com.livecomerce.live.api;

import com.livecomerce.live.domain.ChatBannedException;
import com.livecomerce.live.domain.ChatProviderMisconfiguredException;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.ChatRequestRejectedException;
import com.livecomerce.live.domain.ChatSelfBanException;
import com.livecomerce.live.domain.InvalidLiveStateException;
import com.livecomerce.live.domain.LiveChatRoomNotAvailableException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveNotScheduledException;
import com.livecomerce.live.domain.LiveProductAlreadySoldException;
import com.livecomerce.live.domain.LiveProductNotFoundException;
import com.livecomerce.live.domain.LiveProductOutOfStockException;
import com.livecomerce.live.domain.LiveSubscriptionNotForSellerException;
import com.livecomerce.live.domain.VideoProviderUnavailableException;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@SuppressWarnings("null") // URI.create() is never null; ProblemDetail.setType expects @NonNull
@RestControllerAdvice(basePackages = "com.livecomerce.live")
class LiveExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveExceptionHandler.class);

    @ExceptionHandler(LiveNotFoundException.class)
    ProblemDetail handleNotFound(LiveNotFoundException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-found"));
        detail.setProperty("code", LiveNotFoundException.CODE);
        return detail;
    }

    @ExceptionHandler(InvalidLiveStateException.class)
    ProblemDetail handleInvalidState(InvalidLiveStateException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/invalid-live-state"));
        return detail;
    }

    @ExceptionHandler(LiveNotOwnedBySellerException.class)
    ProblemDetail handleNotOwned(LiveNotOwnedBySellerException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-owned"));
        return detail;
    }

    @ExceptionHandler(LiveProductOutOfStockException.class)
    ProblemDetail handleOutOfStock(LiveProductOutOfStockException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-out-of-stock"));
        return detail;
    }

    @ExceptionHandler(LiveProductNotFoundException.class)
    ProblemDetail handleProductNotFound(LiveProductNotFoundException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-not-found"));
        return detail;
    }

    @ExceptionHandler(LiveNotLiveException.class)
    ProblemDetail handleNotLive(LiveNotLiveException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-active"));
        detail.setProperty("code", LiveNotLiveException.CODE);
        return detail;
    }

    @ExceptionHandler(LiveProductAlreadySoldException.class)
    ProblemDetail handleAlreadySold(LiveProductAlreadySoldException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-already-sold"));
        return detail;
    }

    @ExceptionHandler(LiveNotScheduledException.class)
    ProblemDetail handleNotScheduled(LiveNotScheduledException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-scheduled"));
        return detail;
    }

    @ExceptionHandler(LiveSubscriptionNotForSellerException.class)
    ProblemDetail handleSubscriptionNotForSeller(LiveSubscriptionNotForSellerException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-subscription-buyers-only"));
        return detail;
    }

    @ExceptionHandler(VideoProviderUnavailableException.class)
    ProblemDetail handleVideoProviderUnavailable(VideoProviderUnavailableException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/video-provider-unavailable"));
        return detail;
    }

    @ExceptionHandler(LiveChatRoomNotAvailableException.class)
    ProblemDetail handleChatRoomNotAvailable(LiveChatRoomNotAvailableException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-chat-room-not-available"));
        detail.setProperty("code", LiveChatRoomNotAvailableException.CODE);
        return detail;
    }

    @ExceptionHandler(ChatBannedException.class)
    ProblemDetail handleChatBanned(ChatBannedException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/chat-banned"));
        detail.setProperty("code", ChatBannedException.CODE);
        return detail;
    }

    @ExceptionHandler(ChatSelfBanException.class)
    ProblemDetail handleChatSelfBan(ChatSelfBanException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/chat-self-ban"));
        detail.setProperty("code", ChatSelfBanException.CODE);
        return detail;
    }

    @ExceptionHandler(ChatRequestRejectedException.class)
    ProblemDetail handleChatRequestRejected(ChatRequestRejectedException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/chat-request-rejected"));
        return detail;
    }

    /** Not retryable and not the client's fault (e.g. missing IAM permission): 500 with a generic body. */
    @ExceptionHandler(ChatProviderMisconfiguredException.class)
    ProblemDetail handleChatProviderMisconfigured(ChatProviderMisconfiguredException e) {
        log.error("Chat provider misconfigured: {}", e.getMessage(), e);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "The chat moderation service is not available. Please contact support.");
        detail.setType(URI.create("https://livecomerce.com/errors/chat-provider-misconfigured"));
        return detail;
    }

    @ExceptionHandler(ChatProviderUnavailableException.class)
    ProblemDetail handleChatProviderUnavailable(ChatProviderUnavailableException e) {
        log.error("Chat provider unavailable: {}", e.getMessage(), e);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/chat-provider-unavailable"));
        detail.setProperty("code", ChatProviderUnavailableException.CODE);
        return detail;
    }

    /**
     * The stream-ticket store is down: 503 with {@code Retry-After} and code
     * {@code TICKET_STORE_UNAVAILABLE}, so the client backs off instead of refreshing its session.
     */
    @ExceptionHandler(StreamTicketStoreUnavailableException.class)
    ResponseEntity<ProblemDetail> handleTicketStoreUnavailable(StreamTicketStoreUnavailableException e) {
        log.error("Stream ticket store unavailable ({}); answering 503 {}",
                e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getMessage(),
                StreamTicketStoreUnavailableException.CODE);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Stream tickets are temporarily unavailable. Retry after the Retry-After delay.");
        detail.setType(URI.create("https://livecomerce.com/errors/ticket-store-unavailable"));
        detail.setProperty("code", StreamTicketStoreUnavailableException.CODE);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER,
                        String.valueOf(StreamTicketStoreUnavailableException.RETRY_AFTER.toSeconds()))
                .body(detail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentModification(ObjectOptimisticLockingFailureException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This live was modified by another request at the same time. Please retry.");
        detail.setType(URI.create("https://livecomerce.com/errors/live-concurrent-modification"));
        return detail;
    }
}
