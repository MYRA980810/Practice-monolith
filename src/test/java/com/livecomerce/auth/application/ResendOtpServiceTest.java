package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.ResendOtpUseCase.ResendCommand;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.VerificationChannel;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendOtpServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock VerifyOtpPort verifyOtpPort;

    @InjectMocks ResendOtpService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PENDING_TOKEN = "pending.jwt";

    @Test
    void resend_withPhone_sendsWhatsAppVerification() {
        var user = User.create("u@test.com", "hash", "A", "B", "+5491111", Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));

        service.resend(new ResendCommand(PENDING_TOKEN));

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var channelCaptor = ArgumentCaptor.forClass(VerificationChannel.class);
        verify(verifyOtpPort).send(toCaptor.capture(), channelCaptor.capture());
        assertThat(channelCaptor.getValue()).isEqualTo(VerificationChannel.SMS);
        assertThat(toCaptor.getValue()).isEqualTo("+5491111");
    }

    @Test
    void resend_withoutPhone_sendsEmailVerification() {
        var user = User.create("u@test.com", "hash", "A", "B", null, Role.BUYER);
        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN)).thenReturn(USER_ID);
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));

        service.resend(new ResendCommand(PENDING_TOKEN));

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var channelCaptor = ArgumentCaptor.forClass(VerificationChannel.class);
        verify(verifyOtpPort).send(toCaptor.capture(), channelCaptor.capture());
        assertThat(channelCaptor.getValue()).isEqualTo(VerificationChannel.EMAIL);
        assertThat(toCaptor.getValue()).isEqualTo("u@test.com");
    }

    @Test
    void resend_withInvalidToken_throwsPendingTokenInvalid() {
        when(tokenGeneratorPort.extractUserIdFromPendingToken(PENDING_TOKEN))
                .thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> service.resend(new ResendCommand(PENDING_TOKEN)))
                .isInstanceOf(PendingTokenInvalidException.class);

        verifyNoInteractions(loadUserPort, verifyOtpPort);
    }
}
