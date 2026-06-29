package com.livecomerce.live.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.TrackViewerPresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/agora")
class AgoraWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AgoraWebhookController.class);

    private final AgoraSignatureValidator    signatureValidator;
    private final TrackViewerPresenceService trackViewerPresenceService;
    private final ObjectMapper               objectMapper;
    private final String                     webhookSecret;

    AgoraWebhookController(
            AgoraSignatureValidator signatureValidator,
            TrackViewerPresenceService trackViewerPresenceService,
            ObjectMapper objectMapper,
            @Value("${agora.webhook-secret}") String webhookSecret) {
        this.signatureValidator         = signatureValidator;
        this.trackViewerPresenceService = trackViewerPresenceService;
        this.objectMapper               = objectMapper;
        this.webhookSecret              = webhookSecret;
    }

    @PostMapping
    ResponseEntity<Void> handleWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Agora-Signature-V2", required = false) String signature) {

        if (signature == null || !signatureValidator.validate(webhookSecret, rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var event = objectMapper.readValue(rawBody, AgoraWebhookEvent.class);
            log.info("Agora webhook received: noticeId={}, eventType={}", event.noticeId(), event.eventType());

            if (event.eventType() == 1 && event.payload() != null && event.payload().cname() != null) {
                trackViewerPresenceService.handleJoin(event.payload().cname());
            } else if (event.eventType() == 2 && event.payload() != null && event.payload().cname() != null) {
                trackViewerPresenceService.handleLeave(event.payload().cname());
            }
        } catch (Exception e) {
            log.warn("Failed to parse Agora webhook body: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
