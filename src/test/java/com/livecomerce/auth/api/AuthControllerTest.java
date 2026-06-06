package com.livecomerce.auth.api;

import com.livecomerce.auth.application.EmailAlreadyTakenException;
import com.livecomerce.auth.application.InvalidCredentialsException;
import com.livecomerce.auth.application.OAuthCodeInvalidException;
import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.in.ExchangeOAuthCodeUseCase;
import com.livecomerce.auth.application.port.in.ForgotPasswordUseCase;
import com.livecomerce.auth.application.port.in.PendingVerificationResult;
import com.livecomerce.auth.application.port.in.RegisterUserUseCase;
import com.livecomerce.auth.application.port.in.CompleteOAuthRegistrationUseCase;
import com.livecomerce.auth.application.port.in.ResetPasswordUseCase;
import com.livecomerce.auth.application.port.in.ResetTokenResult;
import com.livecomerce.auth.application.port.in.ResendOtpUseCase;
import com.livecomerce.auth.application.port.in.VerifyOtpUseCase;
import com.livecomerce.auth.application.port.in.VerifyResetCodeUseCase;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.VerificationChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthenticateUserUseCase authenticateUseCase;
    @MockitoBean RegisterUserUseCase registerUseCase;
    @MockitoBean VerifyOtpUseCase verifyOtpUseCase;
    @MockitoBean ResendOtpUseCase resendOtpUseCase;
    @MockitoBean CompleteOAuthRegistrationUseCase completeOAuthUseCase;
    @MockitoBean ForgotPasswordUseCase forgotPasswordUseCase;
    @MockitoBean VerifyResetCodeUseCase verifyResetCodeUseCase;
    @MockitoBean ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase;

    private static final UUID USER_ID = UUID.randomUUID();

    private static final AuthResult AUTH_RESULT = AuthResult.of(
            "jwt-token", USER_ID, "seller@test.com", Role.SELLER
    );

    private static final PendingVerificationResult PENDING_RESULT =
            new PendingVerificationResult("pending-jwt", VerificationChannel.EMAIL);

    // --- LOGIN ---

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        when(authenticateUseCase.authenticate(any())).thenReturn(AUTH_RESULT);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contact":"seller@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.contact").value("seller@test.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void login_withInvalidCredentials_returns401ProblemDetail() throws Exception {
        when(authenticateUseCase.authenticate(any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contact":"seller@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/invalid-credentials"));
    }

    @Test
    void login_withMissingFields_returns400WithErrors() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_withMalformedJson_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/malformed-request"));
    }

    // --- REGISTER ---

    @Test
    void register_withValidData_returns202WithPendingToken() throws Exception {
        when(registerUseCase.register(any())).thenReturn(PENDING_RESULT);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contact":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pendingToken").value("pending-jwt"))
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }

    @Test
    void verifyOtp_withValidCode_returns200WithToken() throws Exception {
        when(verifyOtpUseCase.verify(any())).thenReturn(AUTH_RESULT);

        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pendingToken":"pending-jwt","code":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void resendOtp_withValidToken_returns204() throws Exception {
        doNothing().when(resendOtpUseCase).resend(any());

        mvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pendingToken":"pending-jwt"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void resendOtp_withBlankToken_returns400() throws Exception {
        mvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pendingToken":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_withMissingFields_returns400() throws Exception {
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pendingToken":"","code":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withExistingEmail_returns409ProblemDetail() throws Exception {
        when(registerUseCase.register(any())).thenThrow(new EmailAlreadyTakenException("seller@test.com"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contact":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/email-already-taken"));
    }

    @Test
    void register_withInvalidRole_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"+5491112345678",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/malformed-request"));
    }

    @Test
    void register_withValidationErrors_returns400WithErrorsArray() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"not-an-email",
                                  "password":"123",
                                  "firstName":"",
                                  "lastName":"Doe",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // --- FORGOT PASSWORD ---

    @Test
    void forgotPassword_withValidContact_returns202() throws Exception {
        when(forgotPasswordUseCase.forgotPassword(any()))
                .thenReturn(new PendingVerificationResult("reset-pending-jwt", VerificationChannel.EMAIL));

        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contact":"user@test.com"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pendingToken").value("reset-pending-jwt"))
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }

    // --- VERIFY RESET CODE ---

    @Test
    void verifyResetCode_withValidOtp_returns200WithResetToken() throws Exception {
        when(verifyResetCodeUseCase.verify(any()))
                .thenReturn(new ResetTokenResult("password-reset-jwt"));

        mvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pendingToken":"reset-pending-jwt","code":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("password-reset-jwt"));
    }

    // --- RESET PASSWORD ---

    @Test
    void resetPassword_withMatchingPasswords_returns200WithJwt() throws Exception {
        when(resetPasswordUseCase.reset(any()))
                .thenReturn(AuthResult.of("access-jwt", USER_ID, "user@test.com", Role.BUYER));

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resetToken":"password-reset-jwt",
                                  "newPassword":"newPass123",
                                  "confirmPassword":"newPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // --- OAUTH2 EXCHANGE ---

    @Test
    void exchangeOAuthCode_withValidCode_returns200WithAuthResponse() throws Exception {
        when(exchangeOAuthCodeUseCase.exchange(any()))
                .thenReturn(AuthResult.of("full-jwt", USER_ID, "oauth@test.com", Role.SELLER));

        mvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"valid-exchange-code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("full-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.contact").value("oauth@test.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void exchangeOAuthCode_withBlankCode_returns400WithErrorsArray() throws Exception {
        mvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void exchangeOAuthCode_withMissingBody_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exchangeOAuthCode_whenUseCaseThrowsOAuthCodeInvalid_returns401ProblemDetail() throws Exception {
        when(exchangeOAuthCodeUseCase.exchange(any()))
                .thenThrow(new OAuthCodeInvalidException());

        mvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"expired-or-used-code"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/oauth-code-invalid"));
    }
}
