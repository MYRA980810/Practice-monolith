package com.livecomerce.payment.application.port.out;

import com.livecomerce.payment.domain.BuyerPaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuyerPaymentMethodPort {

    BuyerPaymentMethod save(BuyerPaymentMethod paymentMethod);

    Optional<BuyerPaymentMethod> loadById(UUID id);

    List<BuyerPaymentMethod> loadByUserId(UUID userId);

    int countByUserId(UUID userId);

    Optional<BuyerPaymentMethod> loadDefaultByUserId(UUID userId);

    void clearDefaultForUser(UUID userId);

    void deleteById(UUID id);
}
