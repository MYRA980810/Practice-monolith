package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.SetDefaultBuyerPaymentMethodUseCase;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SetDefaultBuyerPaymentMethodService implements SetDefaultBuyerPaymentMethodUseCase {

    private final BuyerPaymentMethodPort buyerPaymentMethodPort;

    @Override
    public void setDefault(UUID userId, UUID paymentMethodId) {
        var paymentMethod = buyerPaymentMethodPort.loadById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));

        if (!paymentMethod.getUserId().equals(userId)) {
            throw new AccessDeniedException("Payment method does not belong to user");
        }

        buyerPaymentMethodPort.clearDefaultForUser(userId);
        paymentMethod.setAsDefault();
        buyerPaymentMethodPort.save(paymentMethod);
    }
}
