package com.livecomerce.payment.infrastructure.stripe;

import com.livecomerce.payment.application.PaymentGatewayException;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.AccountSession;
import com.stripe.model.Balance;
import com.stripe.model.BankAccount;
import com.stripe.model.Payout;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.AccountSessionCreateParams;
import com.stripe.param.PayoutListParams;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
class StripeConnectGatewayAdapter implements StripeConnectGatewayPort {

    @Override
    public String createExpressAccount(String email) {
        try {
            var paramsBuilder = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS);

            if (email != null) {
                paramsBuilder.setEmail(email);
            }

            var params = paramsBuilder.build();

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

    @Override
    public String createAccountOnboardingSession(String stripeAccountId) {
        try {
            var params = AccountSessionCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setComponents(
                            AccountSessionCreateParams.Components.builder()
                                    .setAccountOnboarding(
                                            AccountSessionCreateParams.Components.AccountOnboarding.builder()
                                                    .setEnabled(true)
                                                    .build())
                                    .build())
                    .build();

            var accountSession = AccountSession.create(params);

            return accountSession.getClientSecret();

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear sesión de onboarding embebido de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public AccountStatus retrieveAccountStatus(String stripeAccountId) {
        try {
            var account = Account.retrieve(stripeAccountId);

            return new AccountStatus(
                    Boolean.TRUE.equals(account.getChargesEnabled()),
                    Boolean.TRUE.equals(account.getPayoutsEnabled()),
                    Boolean.TRUE.equals(account.getDetailsSubmitted()));

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al consultar estado de cuenta Connect de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public AccountDetails retrieveAccountDetails(String stripeAccountId) {
        try {
            var account = Account.retrieve(stripeAccountId);

            var businessProfile = account.getBusinessProfile();
            var requirements = account.getRequirements();

            var bankAccount = resolveBankAccount(account);

            return new AccountDetails(
                    businessProfile != null ? businessProfile.getName() : null,
                    businessProfile != null ? businessProfile.getUrl() : null,
                    account.getEmail(),
                    requirements != null && requirements.getCurrentlyDue() != null ? requirements.getCurrentlyDue() : List.of(),
                    requirements != null && requirements.getPastDue() != null ? requirements.getPastDue() : List.of(),
                    requirements != null ? requirements.getDisabledReason() : null,
                    bankAccount != null ? bankAccount.getBankName() : null,
                    bankAccount != null ? bankAccount.getLast4() : null,
                    bankAccount != null ? bankAccount.getStatus() : null);

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al consultar detalles de cuenta Connect de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public AccountBalance retrieveBalance(String stripeAccountId) {
        try {
            var options = RequestOptions.builder().setStripeAccount(stripeAccountId).build();
            var balance = Balance.retrieve(options);

            var available = balance.getAvailable() != null
                    ? balance.getAvailable().stream()
                            .map(item -> new BalanceAmount(item.getAmount(), item.getCurrency()))
                            .toList()
                    : List.<BalanceAmount>of();

            var pending = balance.getPending() != null
                    ? balance.getPending().stream()
                            .map(item -> new BalanceAmount(item.getAmount(), item.getCurrency()))
                            .toList()
                    : List.<BalanceAmount>of();

            return new AccountBalance(available, pending);

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al consultar balance de cuenta Connect de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public PayoutPage listPayouts(String stripeAccountId, String cursor, int limit) {
        try {
            var paramsBuilder = PayoutListParams.builder()
                    .setLimit((long) limit);

            if (cursor != null && !cursor.isBlank()) {
                paramsBuilder.setStartingAfter(cursor);
            }

            var options = RequestOptions.builder().setStripeAccount(stripeAccountId).build();
            var result = Payout.list(paramsBuilder.build(), options);

            var items = result.getData().stream()
                    .map(payout -> new PayoutItem(
                            payout.getId(),
                            payout.getAmount(),
                            payout.getCurrency(),
                            payout.getStatus(),
                            payout.getMethod(),
                            toOffsetDateTime(payout.getArrivalDate())))
                    .toList();

            boolean hasMore = Boolean.TRUE.equals(result.getHasMore());
            String nextCursor = hasMore && !items.isEmpty() ? items.get(items.size() - 1).id() : null;

            return new PayoutPage(items, nextCursor, hasMore);

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al consultar historial de payouts de Stripe: " + e.getMessage(), e);
        }
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        return epochSeconds != null ? Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC) : null;
    }

    private BankAccount resolveBankAccount(Account account) {
        var externalAccounts = account.getExternalAccounts();
        if (externalAccounts == null || externalAccounts.getData() == null) {
            return null;
        }

        var bankAccounts = externalAccounts.getData().stream()
                .filter(BankAccount.class::isInstance)
                .map(BankAccount.class::cast)
                .toList();

        return bankAccounts.stream()
                .filter(bankAccount -> Boolean.TRUE.equals(bankAccount.getDefaultForCurrency()))
                .findFirst()
                .or(() -> bankAccounts.stream().findFirst())
                .orElse(null);
    }
}
