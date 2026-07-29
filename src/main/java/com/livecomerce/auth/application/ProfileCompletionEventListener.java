package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.payment.BuyerPaymentMethodAttachedEvent;
import com.livecomerce.payment.SellerPayoutAccountActivatedEvent;
import com.livecomerce.store.BuyerAddressAddedEvent;
import com.livecomerce.store.SellerAddressAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCompletionEventListener {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @ApplicationModuleListener
    void on(BuyerAddressAddedEvent event) {
        markAddress(event.userId(), Role.BUYER);
    }

    @ApplicationModuleListener
    void on(SellerAddressAddedEvent event) {
        markAddress(event.userId(), Role.SELLER);
    }

    @ApplicationModuleListener
    void on(BuyerPaymentMethodAttachedEvent event) {
        markPayment(event.userId(), Role.BUYER);
    }

    @ApplicationModuleListener
    void on(SellerPayoutAccountActivatedEvent event) {
        markPayment(event.userId(), Role.SELLER);
    }

    private void markAddress(UUID userId, Role expectedRole) {
        loadUserPort.loadById(userId)
                .filter(u -> u.getRole() == expectedRole)
                .ifPresent(u -> {
                    u.markAddressRequirementMet();
                    saveUserPort.save(u);
                    log.info("Address requirement met — userId={} role={}", userId, expectedRole);
                });
    }

    private void markPayment(UUID userId, Role expectedRole) {
        loadUserPort.loadById(userId)
                .filter(u -> u.getRole() == expectedRole)
                .ifPresent(u -> {
                    u.markPaymentRequirementMet();
                    saveUserPort.save(u);
                    log.info("Payment requirement met — userId={} role={}", userId, expectedRole);
                });
    }
}
