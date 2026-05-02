package com.livecomerce.order.infrastructure.scheduler;

import com.livecomerce.order.application.ReservationExpiryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationExpiryService service;

    @Scheduled(fixedDelayString = "${order.reservation.expiry-check-ms:60000}")
    void expireOverdueReservations() {
        log.debug("Running reservation expiry check");
        service.expireOverdueReservations();
    }
}
