package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.DeleteBuyerPaymentMethodUseCase;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteBuyerPaymentMethodService implements DeleteBuyerPaymentMethodUseCase {

    private final BuyerPaymentMethodPort buyerPaymentMethodPort;
    private final StripePaymentMethodGatewayPort stripePaymentMethodGatewayPort;

    @Override
    public void delete(UUID userId, UUID paymentMethodId) {
        var paymentMethod = buyerPaymentMethodPort.loadById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));

        if (!paymentMethod.getUserId().equals(userId)) {
            throw new AccessDeniedException("Payment method does not belong to user");
        }

        stripePaymentMethodGatewayPort.detachPaymentMethod(paymentMethod.getStripePaymentMethodId());
        buyerPaymentMethodPort.deleteById(paymentMethodId);
    }
}
