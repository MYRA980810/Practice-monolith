package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.VerifyOtpUseCase.VerifyCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.VerificationChannel;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyOtpServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks VerifyOtpService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PENDING_TOKEN = "pending.jwt.token";
    private static final String RAW_CODE = "123456";

    @Test
    void verify_withValidCode_returnsAuthResult() {
        var user = User.create("u@test.com", "hash", "A", "B", "+5491111", Role.BUYER);

        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check("+5491111", RAW_CODE, VerificationChannel.WHATSAPP)).thenReturn(true);
        when(saveUserPort.save(any())).thenReturn(user);
        when(tokenGeneratorPort.generate(any())).thenReturn("full-jwt");

        var result = service.verify(new VerifyCommand(PENDING_TOKEN, RAW_CODE));

        assertThat(result.accessToken()).isEqualTo("full-jwt");
    }

    @Test
    void verify_withInvalidCode_throwsInvalidOtp() {
        var user = User.create("u@test.com", "hash", "A", "B", null, Role.BUYER);

        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check("u@test.com", RAW_CODE, VerificationChannel.EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> service.verify(new VerifyCommand(PENDING_TOKEN, RAW_CODE)))
                .isInstanceOf(InvalidOtpException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    void verify_withExpiredOtp_throwsInvalidOtp() {
        // Twilio handles expiry server-side; an expired code returns false from check()
        var user = User.create("u@test.com", "hash", "A", "B", null, Role.BUYER);

        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check("u@test.com", RAW_CODE, VerificationChannel.EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> service.verify(new VerifyCommand(PENDING_TOKEN, RAW_CODE)))
                .isInstanceOf(InvalidOtpException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    void verify_withInvalidPendingToken_throwsPendingTokenInvalid() {
        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN))
                .thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> service.verify(new VerifyCommand(PENDING_TOKEN, RAW_CODE)))
                .isInstanceOf(PendingTokenInvalidException.class);
    }

    @Test
    void verify_withValidCode_marksUserAsVerified() {
        var user = User.create("u@test.com", "hash", "A", "B", null, Role.BUYER);

        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(verifyOtpPort.check("u@test.com", RAW_CODE, VerificationChannel.EMAIL)).thenReturn(true);
        when(saveUserPort.save(any())).thenReturn(user);
        when(tokenGeneratorPort.generate(any())).thenReturn("jwt");

        service.verify(new VerifyCommand(PENDING_TOKEN, RAW_CODE));

        assertThat(user.isVerified()).isTrue();
    }
}
