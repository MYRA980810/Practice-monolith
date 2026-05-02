package com.livecomerce.billing.infrastructure.job;

import com.livecomerce.billing.application.port.out.LoadSubscriptionPort;
import com.livecomerce.billing.application.port.out.SaveSubscriptionPort;
import com.livecomerce.billing.domain.SubscriptionExpiredEvent;
import com.livecomerce.billing.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DowngradeExpiredSubscriptionsJob {

    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SaveSubscriptionPort saveSubscriptionPort;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void run() {
        var expired = loadSubscriptionPort.loadByStatusAndPeriodEndBefore(
                SubscriptionStatus.ACTIVE, OffsetDateTime.now());

        log.info("Downgrade job: {} suscripciones expiradas encontradas", expired.size());

        expired.forEach(subscription -> {
            subscription.expire();
            saveSubscriptionPort.save(subscription);
            eventPublisher.publishEvent(new SubscriptionExpiredEvent(subscription.getUserId()));
            log.info("Suscripción expirada — userId={}", subscription.getUserId());
        });
    }
}
