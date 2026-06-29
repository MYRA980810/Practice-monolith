package com.livecomerce.live.infrastructure.agora;

import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
class AgoraRtmRestAdapter implements AgoraRtmMessagePort {

    private static final Logger log      = LoggerFactory.getLogger(AgoraRtmRestAdapter.class);
    private static final String SENDER_ID = "live-server";

    private final RestClient restClient;
    private final String     appId;
    private final String     authHeader;

    @Autowired
    AgoraRtmRestAdapter(
            @Value("${agora.app-id}") String appId,
            @Value("${agora.customer-id}") String customerId,
            @Value("${agora.customer-secret}") String customerSecret) {
        this(
            RestClient.builder().baseUrl("https://api.agora.io").build(),
            appId,
            customerId,
            customerSecret
        );
    }

    AgoraRtmRestAdapter(RestClient restClient, String appId, String customerId, String customerSecret) {
        this.restClient = restClient;
        this.appId      = appId;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((customerId + ":" + customerSecret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void sendChannelMessage(String channelName, String payloadJson) {
        try {
            var body = Map.of(
                    "channel_name",               channelName,
                    "enable_historical_messaging", false,
                    "payload",                     payloadJson
            );
            restClient.post()
                    .uri("/dev/v2/project/{appId}/rtm/users/{senderId}/channel_messages", appId, SENDER_ID)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Agora RTM channel message to '{}' failed: {}", channelName, e.getMessage());
        }
    }

    @Override
    public void sendPeerMessage(String userId, String payloadJson) {
        try {
            var body = Map.of(
                    "destination",                 userId,
                    "enable_offline_messaging",    false,
                    "enable_historical_messaging", false,
                    "payload",                     payloadJson
            );
            restClient.post()
                    .uri("/dev/v2/project/{appId}/rtm/users/{senderId}/peer_messages", appId, SENDER_ID)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Agora RTM peer message to user '{}' failed: {}", userId, e.getMessage());
        }
    }
}
