package com.livecomerce.live.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks/agora/signaling")
public class AgoraSignalingWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AgoraSignalingWebhookController.class);

    private static final String CHANNEL_PREFIX = "live-chat:";

    private final AgoraSignatureValidator  signatureValidator;
    private final SaveChatMessageUseCase   saveChatMessageUseCase;
    private final ObjectMapper             objectMapper;
    private final String                   webhookSecret;

    AgoraSignalingWebhookController(
            AgoraSignatureValidator signatureValidator,
            SaveChatMessageUseCase saveChatMessageUseCase,
            ObjectMapper objectMapper,
            @Value("${agora.webhook-secret}") String webhookSecret) {
        this.signatureValidator   = signatureValidator;
        this.saveChatMessageUseCase = saveChatMessageUseCase;
        this.objectMapper         = objectMapper;
        this.webhookSecret        = webhookSecret;
    }

    @PostMapping
    ResponseEntity<Void> handleSignalingEvent(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Agora-Signature-V2", required = false) String signature) {

        if (signature == null || !signatureValidator.validate(webhookSecret, rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var event = objectMapper.readValue(rawBody, AgoraSignalingEvent.class);
            processEvent(event);
        } catch (Exception e) {
            log.warn("Failed to parse or process Agora signaling event: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private void processEvent(AgoraSignalingEvent event) {
        if (event.channel() == null || !event.channel().startsWith(CHANNEL_PREFIX)) {
            log.debug("Skipping signaling event: channel '{}' not a live-chat channel", event.channel());
            return;
        }

        try {
            var liveIdStr = event.channel().substring(CHANNEL_PREFIX.length());
            var liveId    = UUID.fromString(liveIdStr);
            var userId    = UUID.fromString(event.userId());
            var sentAt    = OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.sendTs()), ZoneOffset.UTC);

            saveChatMessageUseCase.saveChatMessage(new SaveChatMessageUseCase.SaveChatMessageCommand(
                    liveId,
                    event.messageId(),
                    userId,
                    event.userId(),   // username = RTM user ID (user's UUID string)
                    event.message(),
                    sentAt
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Skipping signaling event: invalid liveId or userId in channel '{}': {}",
                    event.channel(), e.getMessage());
        }
    }
}
