package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.ChatRoomPort;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.CreateRoomRequest;
import software.amazon.awssdk.services.ivschat.model.DeleteRoomRequest;
import software.amazon.awssdk.services.ivschat.model.MessageReviewHandler;
import software.amazon.awssdk.services.ivschat.model.ResourceNotFoundException;

import java.util.Map;
import java.util.UUID;

@Component
class IvsChatRoomAdapter implements ChatRoomPort {

    private final IvschatClient ivschatClient;
    private final int           maxMessageLength;
    private final int           maxMessagesPerSecond;
    private final String        reviewHandlerArn;
    private final String        fallbackResult;

    IvsChatRoomAdapter(
            IvschatClient ivschatClient,
            @Value("${ivs.chat.max-message-length:500}") int maxMessageLength,
            @Value("${ivs.chat.max-messages-per-second:10}") int maxMessagesPerSecond,
            @Value("${ivs.chat.review-handler-arn:}") String reviewHandlerArn,
            @Value("${ivs.chat.fallback-result:ALLOW}") String fallbackResult) {
        this.ivschatClient        = ivschatClient;
        this.maxMessageLength     = maxMessageLength;
        this.maxMessagesPerSecond = maxMessagesPerSecond;
        this.reviewHandlerArn     = reviewHandlerArn;
        this.fallbackResult       = fallbackResult;
    }

    @Override
    public String createRoom(UUID liveId, UUID sellerId) {
        var request = CreateRoomRequest.builder()
                .name("live-" + liveId)
                .maximumMessageLength(maxMessageLength)
                .maximumMessageRatePerSecond(maxMessagesPerSecond)
                .tags(Map.of(
                        "liveId",   liveId.toString(),
                        "sellerId", sellerId.toString()));

        // Without a review handler the room accepts every message as-is (no moderation).
        if (reviewHandlerArn != null && !reviewHandlerArn.isBlank()) {
            request.messageReviewHandler(MessageReviewHandler.builder()
                    .uri(reviewHandlerArn)
                    .fallbackResult(fallbackResult)
                    .build());
        }

        try {
            return ivschatClient.createRoom(request.build()).arn();
        } catch (SdkException e) {
            throw new ChatProviderUnavailableException("Unable to create chat room for live " + liveId, e);
        }
    }

    @Override
    public void deleteRoom(String roomArn) {
        try {
            ivschatClient.deleteRoom(DeleteRoomRequest.builder().identifier(roomArn).build());
        } catch (ResourceNotFoundException e) {
            // room already deleted, nothing to do
        }
    }
}
