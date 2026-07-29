package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateSetupIntentUseCase;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.application.port.out.BuyerStripeCustomerPort;
import com.livecomerce.payment.application.port.out.StripeCustomerGatewayPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import com.livecomerce.payment.domain.BuyerStripeCustomer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateSetupIntentService implements CreateSetupIntentUseCase {

    private final BuyerStripeCustomerPort buyerStripeCustomerPort;
    private final BuyerPaymentMethodPort buyerPaymentMethodPort;
    private final StripeCustomerGatewayPort stripeCustomerGatewayPort;
    private final StripePaymentMethodGatewayPort stripePaymentMethodGatewayPort;

    @Override
    public SetupIntentResult createSetupIntent(CreateSetupIntentCommand cmd) {
        var stripeCustomerId = buyerStripeCustomerPort.loadByUserId(cmd.buyerId())
                .map(BuyerStripeCustomer::getStripeCustomerId)
                .orElseGet(() -> {
                    var customerId = stripeCustomerGatewayPort.createCustomer(cmd.email());
                    buyerStripeCustomerPort.save(BuyerStripeCustomer.create(cmd.buyerId(), customerId));
                    return customerId;
                });

        if (buyerPaymentMethodPort.countByUserId(cmd.buyerId()) >= 6) {
            throw new PaymentMethodLimitExceededException(cmd.buyerId());
        }

        var handle = stripePaymentMethodGatewayPort.createSetupIntent(stripeCustomerId);

        return new SetupIntentResult(handle.clientSecret());
    }
}
