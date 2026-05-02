package com.livecomerce.store.application;

import com.livecomerce.billing.domain.PaymentConfirmedEvent;
import com.livecomerce.billing.domain.SubscriptionExpiredEvent;
import com.livecomerce.shared.Plan;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;

    @ApplicationModuleListener
    void on(PaymentConfirmedEvent event) {
        loadStorePort.loadByUserId(event.userId()).ifPresent(store -> {
            store.changePlan(event.plan());
            saveStorePort.save(store);
            log.info("Plan actualizado — userId={} plan={}", event.userId(), event.plan());
        });
    }

    @ApplicationModuleListener
    void on(SubscriptionExpiredEvent event) {
        loadStorePort.loadByUserId(event.userId()).ifPresent(store -> {
            store.changePlan(Plan.FREE);
            saveStorePort.save(store);
            log.info("Plan degradado a FREE — userId={}", event.userId());
        });
    }
}
