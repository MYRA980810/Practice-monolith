package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.IvsChatTokenPort;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.ChatTokenCapability;
import software.amazon.awssdk.services.ivschat.model.CreateChatTokenRequest;

import java.util.List;
import java.util.Map;

@Component
class IvsChatTokenAdapter implements IvsChatTokenPort {

    private static final Logger log = LoggerFactory.getLogger(IvsChatTokenAdapter.class);

    private final IvschatClient ivschatClient;
    private final String        region;

    IvsChatTokenAdapter(IvschatClient ivschatClient, @Value("${ivs.region}") String region) {
        this.ivschatClient = ivschatClient;
        this.region        = region;
    }

    @Override
    public ChatToken createToken(String roomArn, String userId, Map<String, String> attributes,
                                 boolean canSend, int sessionDurationMinutes) {
        var request = CreateChatTokenRequest.builder()
                .roomIdentifier(roomArn)
                .userId(userId)
                // At most SEND_MESSAGE, for sellers too: deleting messages and banning go through the backend.
                // No capability at all is a receive-only token: viewing messages and events is always implied.
                .capabilities(canSend ? List.of(ChatTokenCapability.SEND_MESSAGE) : List.of())
                .attributes(attributes)
                .sessionDurationInMinutes(sessionDurationMinutes)
                .build();
        try {
            var response = ivschatClient.createChatToken(request);
            return new ChatToken(response.token(), response.sessionExpirationTime(),
                    response.tokenExpirationTime(), region);
        } catch (SdkException e) {
            // The room ARN stays server-side: the exception message is returned to the client.
            log.warn("IVS Chat createChatToken failed for room {}", roomArn, e);
            throw new ChatProviderUnavailableException("Unable to create chat token", e);
        }
    }
}
