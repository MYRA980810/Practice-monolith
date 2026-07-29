package com.livecomerce.payment.infrastructure.stripe;

import com.livecomerce.payment.application.PaymentGatewayException;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.stereotype.Component;

@Component
class StripeConnectGatewayAdapter implements StripeConnectGatewayPort {

    @Override
    public String createExpressAccount(String email) {
        try {
            var params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setEmail(email)
                    .build();

            var account = Account.create(params);

            return account.getId();

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear cuenta Connect Express de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl) {
        try {
            var params = AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            var accountLink = AccountLink.create(params);

            return accountLink.getUrl();

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear account link de Stripe: " + e.getMessage(), e);
        }
    }
}
