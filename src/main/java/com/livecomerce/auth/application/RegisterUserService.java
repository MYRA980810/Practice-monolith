package com.livecomerce.auth.application;

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
        if (loadUserPort.loadByEmail(command.email()).isPresent()) {
            throw new EmailAlreadyTakenException(command.email());
        }

        var user = User.create(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.firstName(),
                command.lastName(),
                command.phone(),
                command.role()
        );

        var saved = saveUserPort.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmail(), saved.getRole()));

        var channel = saved.resolveChannel();
        verifyOtpPort.send(saved.resolveRecipient(), channel);

        return new PendingVerificationResult(tokenGeneratorPort.generatePendingToken(saved), channel);
    }
}
