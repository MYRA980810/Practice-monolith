package com.livecomerce.auth.application;

import com.livecomerce.auth.api.validation.ContactValidator;
import com.livecomerce.auth.application.port.in.ForgotPasswordUseCase;
import com.livecomerce.auth.application.port.in.PendingVerificationResult;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordService implements ForgotPasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public PendingVerificationResult forgotPassword(ForgotPasswordCommand command) {
        var contact = command.contact();
        boolean isPhone = ContactValidator.isPhone(contact);
        var user = isPhone
                ? loadUserPort.loadByPhone(contact).orElseThrow(ContactNotFoundException::new)
                : loadUserPort.loadByEmail(contact).orElseThrow(ContactNotFoundException::new);
        var channel = user.resolveChannel();
        verifyOtpPort.send(user.resolveRecipient(), channel);
        return new PendingVerificationResult(tokenGeneratorPort.generateResetPendingToken(user), channel);
    }
}
