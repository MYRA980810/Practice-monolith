package com.livecomerce.auth.api;

import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.in.CompleteOAuthRegistrationUseCase;
import com.livecomerce.auth.application.port.in.ExchangeOAuthCodeUseCase;
import com.livecomerce.auth.application.port.in.ForgotPasswordUseCase;
import com.livecomerce.auth.application.port.in.LogoutUseCase;
import com.livecomerce.auth.application.port.in.RefreshAccessTokenUseCase;
import com.livecomerce.auth.application.port.in.RegisterUserUseCase;
import com.livecomerce.auth.application.port.in.ResendOtpUseCase;
import com.livecomerce.auth.application.port.in.ResetPasswordUseCase;
import com.livecomerce.auth.application.port.in.UpdateUserAvatarUseCase;
import com.livecomerce.auth.application.port.in.UpdateUserAvatarUseCase.UpdateUserAvatarCommand;
import com.livecomerce.auth.application.port.in.VerifyOtpUseCase;
import com.livecomerce.auth.application.port.in.VerifyResetCodeUseCase;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String CSRF_HEADER = "X-Requested-With";

    private final RegisterUserUseCase registerUseCase;
    private final AuthenticateUserUseCase authenticateUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final CompleteOAuthRegistrationUseCase completeOAuthUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final VerifyResetCodeUseCase verifyResetCodeUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase;
    private final UpdateUserAvatarUseCase updateUserAvatarUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

    @PostMapping("/register")
    ResponseEntity<VerificationInitiatedResponse> register(@Valid @RequestBody RegisterRequest request) {
        var result = registerUseCase.register(new RegisterUserUseCase.RegisterCommand(
                request.contact(), request.password(), request.firstName(),
                request.lastName(), request.role()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VerificationInitiatedResponse.from(result));
    }

    @PostMapping("/resend-otp")
    ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        resendOtpUseCase.resend(new ResendOtpUseCase.ResendCommand(request.pendingToken()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-otp")
    ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        var result = verifyOtpUseCase.verify(new VerifyOtpUseCase.VerifyCommand(
                request.pendingToken(), request.code()
        ));
        return authResponseWithCookie(result);
    }

    @PostMapping("/oauth2/complete")
    ResponseEntity<AuthResponse> completeOAuth(@Valid @RequestBody CompleteOAuthRequest request) {
        var result = completeOAuthUseCase.complete(new CompleteOAuthRegistrationUseCase.CompleteOAuthCommand(
                request.pendingToken(), request.role()
        ));
        return authResponseWithCookie(result);
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUseCase.authenticate(
                new AuthenticateUserUseCase.AuthCommand(request.contact(), request.password())
        );
        return authResponseWithCookie(result);
    }

    @PostMapping("/forgot-password")
    ResponseEntity<VerificationInitiatedResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        var result = forgotPasswordUseCase.forgotPassword(
                new ForgotPasswordUseCase.ForgotPasswordCommand(request.contact()));
        return ResponseEntity.accepted().body(VerificationInitiatedResponse.from(result));
    }

    @PostMapping("/verify-reset-code")
    ResponseEntity<PasswordResetTokenResponse> verifyResetCode(
            @Valid @RequestBody VerifyResetRequest request) {
        var result = verifyResetCodeUseCase.verify(
                new VerifyResetCodeUseCase.VerifyResetCommand(request.pendingToken(), request.code()));
        return ResponseEntity.ok(new PasswordResetTokenResponse(result.resetToken()));
    }

    @PostMapping("/reset-password")
    ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        var result = resetPasswordUseCase.reset(
                new ResetPasswordUseCase.ResetPasswordCommand(
                        request.resetToken(), request.newPassword(), request.confirmPassword()));
        return authResponseWithCookie(result);
    }

    @PostMapping("/oauth2/exchange")
    ResponseEntity<AuthResponse> exchangeOAuthCode(@Valid @RequestBody ExchangeCodeRequest request) {
        var result = exchangeOAuthCodeUseCase.exchange(
                new ExchangeOAuthCodeUseCase.ExchangeCommand(request.code()));
        return authResponseWithCookie(result);
    }

    @PatchMapping("/me/avatar")
    ResponseEntity<UpdateAvatarResponse> updateAvatar(
            @Valid @RequestBody UpdateAvatarRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var avatarUrl = updateUserAvatarUseCase.updateAvatar(
                new UpdateUserAvatarCommand(principal.getUserId(), request.avatarUrl()));
        return ResponseEntity.ok(new UpdateAvatarResponse(avatarUrl));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshCookie,
            @RequestHeader(value = CSRF_HEADER, required = false) String csrfHeader) {

        if (csrfHeader == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (refreshCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var result = refreshAccessTokenUseCase.refresh(refreshCookie);
        var cookie = buildRefreshCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(result.accessToken(), "Bearer", null, null, null, null));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshCookie,
            @RequestHeader(value = CSRF_HEADER, required = false) String csrfHeader) {

        if (csrfHeader == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logoutUseCase.logout(refreshCookie);

        var clearCookie = buildCookie("", 0);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    // --- helpers ---

    private ResponseEntity<AuthResponse> authResponseWithCookie(com.livecomerce.auth.application.port.in.AuthResult result) {
        var body = ResponseEntity.ok();
        if (result.refreshToken() != null) {
            var cookie = buildRefreshCookie(result.refreshToken());
            body = body.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return body.body(AuthResponse.from(result));
    }

    private ResponseCookie buildRefreshCookie(String rawToken) {
        return buildCookie(rawToken, refreshExpirationMs / 1000);
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
