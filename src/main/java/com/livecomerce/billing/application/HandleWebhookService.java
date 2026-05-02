package com.livecomerce.billing.application;

import com.livecomerce.billing.application.port.in.HandleWebhookUseCase;
import com.livecomerce.billing.application.port.out.LoadSubscriptionPort;
import com.livecomerce.billing.application.port.out.PaymentGatewayPort;
import com.livecomerce.billing.application.port.out.SaveSubscriptionPort;
import com.livecomerce.billing.domain.PaymentConfirmedEvent;
import com.livecomerce.billing.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HandleWebhookService implements HandleWebhookUseCase {

    private final List<PaymentGatewayPort> gateways;
    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SaveSubscriptionPort saveSubscriptionPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(WebhookCommand command) {
        var gateway = gateways.stream()
                .filter(g -> g.gateway() == command.gateway())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gateway no soportado: " + command.gateway()));

        var event = gateway.parseWebhook(command.payload(), command.signature());

        switch (event.type()) {
            case PAYMENT_SUCCEEDED, SUBSCRIPTION_RENEWED -> handlePaymentSuccess(gateway.gateway(), event);
            case SUBSCRIPTION_CANCELLED -> handleCancellation(event);
        }
    }

    private void handlePaymentSuccess(com.livecomerce.billing.domain.Gateway gateway,
                                      PaymentGatewayPort.WebhookEvent event) {
        var subscription = loadSubscriptionPort.loadByUserId(event.userId())
                .map(s -> { s.renew(event.externalId(), event.periodEnd()); return s; })
                .orElseGet(() -> Subscription.create(
                        event.userId(), event.plan(),
                        gateway, event.externalId(), event.periodEnd()));

        saveSubscriptionPort.save(subscription);
        eventPublisher.publishEvent(new PaymentConfirmedEvent(event.userId(), event.plan()));
    }

    private void handleCancellation(PaymentGatewayPort.WebhookEvent event) {
        loadSubscriptionPort.loadByUserId(event.userId()).ifPresent(s -> {
            s.cancel();
            saveSubscriptionPort.save(s);
        });
    }
}
