package com.livecomerce.billing.application;

import com.livecomerce.billing.application.port.in.GetSubscriptionUseCase;
import com.livecomerce.billing.application.port.out.LoadSubscriptionPort;
import com.livecomerce.billing.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSubscriptionService implements GetSubscriptionUseCase {

    private final LoadSubscriptionPort loadSubscriptionPort;

    @Override
    public Subscription getByUserId(UUID userId) {
        return loadSubscriptionPort.loadByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException(userId.toString()));
    }
}
