package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.ChatModerationPort;
import com.livecomerce.live.domain.ChatProviderMisconfiguredException;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.ChatRequestRejectedException;
import com.livecomerce.live.domain.ChatRoomNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.AccessDeniedException;
import software.amazon.awssdk.services.ivschat.model.DeleteMessageRequest;
import software.amazon.awssdk.services.ivschat.model.DisconnectUserRequest;
import software.amazon.awssdk.services.ivschat.model.PendingVerificationException;
import software.amazon.awssdk.services.ivschat.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ivschat.model.ValidationException;

/**
 * Requires the IAM permissions {@code ivschat:DisconnectUser} and {@code ivschat:DeleteMessage}.
 * Error mapping: validation → rejected (400); access denied / pending verification → misconfigured (500);
 * everything else (throttling, 5xx, timeouts, network) → unavailable (503, retryable).
 * Room ARNs are logged, never put in exception messages (those reach the client).
 */
@Component
class IvsChatModerationAdapter implements ChatModerationPort {

    private static final Logger log = LoggerFactory.getLogger(IvsChatModerationAdapter.class);

    private static final String DISCONNECT_USER = "DisconnectUser";
    private static final String DELETE_MESSAGE  = "DeleteMessage";

    private final IvschatClient ivschatClient;

    IvsChatModerationAdapter(IvschatClient ivschatClient) {
        this.ivschatClient = ivschatClient;
    }

    @Override
    public void disconnectUser(String roomArn, String userId, String reason) {
        var request = DisconnectUserRequest.builder()
                .roomIdentifier(roomArn)
                .userId(userId)
                .reason(reason)
                .build();
        try {
            ivschatClient.disconnectUser(request);
        } catch (ResourceNotFoundException e) {
            // The room is gone (live ended), so nobody can still be connected to it.
            log.info("IVS Chat {} skipped, room {} not found", DISCONNECT_USER, roomArn);
        } catch (SdkException e) {
            throw translate(DISCONNECT_USER, roomArn, e);
        }
    }

    @Override
    public void deleteMessage(String roomArn, String messageId) {
        var request = DeleteMessageRequest.builder()
                .roomIdentifier(roomArn)
                .id(messageId)
                .build();
        try {
            ivschatClient.deleteMessage(request);
        } catch (ResourceNotFoundException e) {
            // The live ended (and its room was deleted) between our status check and this call.
            log.info("IVS Chat {} failed, room {} not found", DELETE_MESSAGE, roomArn);
            throw new ChatRoomNotFoundException(e);
        } catch (SdkException e) {
            throw translate(DELETE_MESSAGE, roomArn, e);
        }
    }

    private static RuntimeException translate(String action, String roomArn, SdkException e) {
        if (e instanceof ValidationException) {
            log.warn("IVS Chat {} rejected as invalid for room {}", action, roomArn, e);
            return new ChatRequestRejectedException(e);
        }
        if (e instanceof AccessDeniedException || e instanceof PendingVerificationException) {
            log.error("IVS Chat {} refused for room {}: check the ivschat:{} IAM permission and the account status",
                    action, roomArn, action, e);
            return new ChatProviderMisconfiguredException(action, e);
        }
        log.warn("IVS Chat {} failed for room {}", action, roomArn, e);
        return new ChatProviderUnavailableException("Unable to perform chat moderation (" + action + ")", e);
    }
}
