package com.livecomerce.auth.api;

import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.in.CompleteOAuthRegistrationUseCase;
import com.livecomerce.auth.application.port.in.ForgotPasswordUseCase;
import com.livecomerce.auth.application.port.in.RegisterUserUseCase;
import com.livecomerce.auth.application.port.in.ResetPasswordUseCase;
import com.livecomerce.auth.application.port.in.ResendOtpUseCase;
import com.livecomerce.auth.application.port.in.VerifyOtpUseCase;
import com.livecomerce.auth.application.port.in.VerifyResetCodeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {

    private final RegisterUserUseCase registerUseCase;
    private final AuthenticateUserUseCase authenticateUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final CompleteOAuthRegistrationUseCase completeOAuthUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final VerifyResetCodeUseCase verifyResetCodeUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

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
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @PostMapping("/oauth2/complete")
    ResponseEntity<AuthResponse> completeOAuth(@Valid @RequestBody CompleteOAuthRequest request) {
        var result = completeOAuthUseCase.complete(new CompleteOAuthRegistrationUseCase.CompleteOAuthCommand(
                request.pendingToken(), request.role()
        ));
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUseCase.authenticate(
                new AuthenticateUserUseCase.AuthCommand(request.contact(), request.password())
        );
        return ResponseEntity.ok(AuthResponse.from(result));
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
        return ResponseEntity.ok(AuthResponse.from(result));
    }
}
