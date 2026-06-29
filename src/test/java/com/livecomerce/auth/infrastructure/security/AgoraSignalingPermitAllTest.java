package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.live.api.AgoraSignalingWebhookController;
import com.livecomerce.live.api.AgoraSignatureValidator;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that /api/webhooks/agora/** is accessible without a JWT token.
 * Must be in the auth.infrastructure.security package to reference package-private bean types.
 */
@SuppressWarnings("null")
@WebMvcTest(
        controllers = AgoraSignalingWebhookController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "agora.webhook-secret=test-webhook-secret",
        "agora.app-id=970ca35de60c44645bbb8c4b22f1d6b9",
        "agora.app-certificate=5cfd2fd1755d40ecb72977518be15d3b",
        "jwt.secret=livecomerce-secret-key-change-this-in-production-min-32chars",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=2592000000"
})
class AgoraSignalingPermitAllTest {

    @Autowired
    MockMvc mvc;

    // Package-private beans in auth.infrastructure.security — accessible from this package
    @MockitoBean JwtService                              jwtService;
    @MockitoBean UserDetailsAdapter                      userDetailsAdapter;
    @MockitoBean GoogleOAuth2SuccessHandler              googleOAuth2SuccessHandler;
    @MockitoBean CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    @MockitoBean RestAuthenticationEntryPoint            restAuthenticationEntryPoint;

    // OAuth2 login requires a ClientRegistrationRepository even when autoconfiguration is excluded
    @MockitoBean ClientRegistrationRepository            clientRegistrationRepository;

    // Live module beans needed by the controller under test
    @MockitoBean AgoraSignatureValidator  signatureValidator;
    @MockitoBean SaveChatMessageUseCase   saveChatMessageUseCase;

    @Test
    void signalingWebhook_withValidSignature_andNoJwt_isNotRejectedWith401() throws Exception {
        when(signatureValidator.validate(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/webhooks/agora/signaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Agora-Signature-V2", "valid-signature")
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
