package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = AgoraSignalingWebhookController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(AgoraSignalingWebhookControllerTest.EmptyConfig.class)
@TestPropertySource(properties = "agora.webhook-secret=test-webhook-secret")
class AgoraSignalingWebhookControllerTest {

    @TestConfiguration
    static class EmptyConfig implements WebMvcConfigurer {}

    @Autowired MockMvc mvc;

    @MockitoBean AgoraSignatureValidator signatureValidator;
    @MockitoBean SaveChatMessageUseCase  saveChatMessageUseCase;

    private static final String VALID_SIGNALING_BODY = """
            {
              "channel": "live-chat:550e8400-e29b-41d4-a716-446655440000",
              "userId": "770e8400-e29b-41d4-a716-446655440111",
              "message": "Hello everyone!",
              "messageId": "agora-msg-abc-123",
              "sendTs": 1700000000000,
              "eventType": "MESSAGE"
            }
            """;

    @Test
    void handleSignalingEvent_withValidSignature_returns200() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-sig")
                        .content(VALID_SIGNALING_BODY))
                .andExpect(status().isOk());

        verify(saveChatMessageUseCase).saveChatMessage(any());
    }

    @Test
    void handleSignalingEvent_withInvalidSignature_returns401() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(false);

        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "bad-sig")
                        .content(VALID_SIGNALING_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void handleSignalingEvent_withMissingSignatureHeader_returns401() throws Exception {
        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SIGNALING_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleSignalingEvent_withNonLiveChatChannel_returns200WithoutPersisting() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        var otherChannelBody = """
                {"channel":"rtc-channel","userId":"user-id","message":"Hi","messageId":"m1","sendTs":1700000000000,"eventType":"MSG"}
                """;

        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-sig")
                        .content(otherChannelBody))
                .andExpect(status().isOk());

        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void handleSignalingEvent_withMalformedBody_returns200() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-sig")
                        .content("not-valid-json"))
                .andExpect(status().isOk());
    }
}
