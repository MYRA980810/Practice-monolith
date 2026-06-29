package com.livecomerce.live.api;

import com.livecomerce.live.application.TrackViewerPresenceService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = AgoraWebhookController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(AgoraWebhookControllerTest.EmptyConfig.class)
class AgoraWebhookControllerTest {

    @TestConfiguration
    static class EmptyConfig implements WebMvcConfigurer {
        // no-op — webhook has no @AuthenticationPrincipal
    }

    @Autowired MockMvc mvc;

    @MockitoBean AgoraSignatureValidator    signatureValidator;
    @MockitoBean TrackViewerPresenceService trackViewerPresenceService;

    private static final String VALID_BODY = """
            {"noticeId":"abc-123","productId":1,"eventType":1}
            """;

    // --- POST /api/webhooks/agora ---

    @Test
    void handleWebhook_withValidSignature_returns200() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/webhooks/agora")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-signature")
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void handleWebhook_withInvalidSignature_returns401() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(false);

        mvc.perform(post("/api/webhooks/agora")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "bad-signature")
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleWebhook_withMissingSignatureHeader_returns401() throws Exception {
        mvc.perform(post("/api/webhooks/agora")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleWebhook_withMalformedBody_stillReturns200WhenSignatureValid() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/webhooks/agora")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-signature")
                        .content("not valid json"))
                .andExpect(status().isOk());
    }
}
