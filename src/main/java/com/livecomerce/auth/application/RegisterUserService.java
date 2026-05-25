package com.livecomerce.auth.application;

import com.livecomerce.auth.api.validation.ContactValidator;
import com.livecomerce.auth.application.port.in.PendingVerificationResult;
import com.livecomerce.auth.application.port.in.RegisterUserUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.domain.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final VerifyOtpPort verifyOtpPort;

    @Override
    public PendingVerificationResult register(RegisterCommand command) {
        var contact = command.contact();
        boolean isPhone = ContactValidator.isPhone(contact);

        if (isPhone) {
            if (loadUserPort.loadByPhone(contact).isPresent()) throw new PhoneAlreadyTakenException(contact);
        } else {
            if (loadUserPort.loadByEmail(contact).isPresent()) throw new EmailAlreadyTakenException(contact);
        }

        var user = User.create(
                isPhone ? null : contact,
                passwordEncoder.encode(command.password()),
                command.firstName(),
                command.lastName(),
                isPhone ? contact : null,
                command.role()
        );

        var saved = saveUserPort.save(user);
        var contactValue = saved.getEmail() != null ? saved.getEmail() : saved.getPhone();
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), contactValue, saved.getRole()));

        var channel = saved.resolveChannel();
        verifyOtpPort.send(saved.resolveRecipient(), channel);

        return new PendingVerificationResult(tokenGeneratorPort.generatePendingToken(saved), channel);
    }
}
