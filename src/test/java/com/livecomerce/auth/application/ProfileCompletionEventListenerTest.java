package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.SaveUserPort;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.payment.BuyerPaymentMethodAttachedEvent;
import com.livecomerce.payment.SellerPayoutAccountActivatedEvent;
import com.livecomerce.store.BuyerAddressAddedEvent;
import com.livecomerce.store.SellerAddressAddedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileCompletionEventListenerTest {

    @Mock LoadUserPort loadUserPort;
    @Mock SaveUserPort saveUserPort;
    @InjectMocks ProfileCompletionEventListener sut;

    private static User user(Role role) {
        return User.create("user@example.com", "hash", "First", "Last", null, role);
    }

    @Test
    void onBuyerAddressAdded_whenUserIsSeller_doesNotMarkAnything() {
        var userId = UUID.randomUUID();
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user(Role.SELLER)));

        sut.on(new BuyerAddressAddedEvent(userId));

        verify(saveUserPort, never()).save(any());
    }

    @Test
    void onBuyerAddressAdded_whenUserIsBuyer_marksAddressRequirementMet() {
        var userId = UUID.randomUUID();
        var buyer = user(Role.BUYER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(buyer));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new BuyerAddressAddedEvent(userId));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().isAddressRequirementMet()).isTrue();
    }

    @Test
    void onSellerAddressAdded_whenUserIsBuyer_doesNotMarkAnything() {
        var userId = UUID.randomUUID();
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(user(Role.BUYER)));

        sut.on(new SellerAddressAddedEvent(userId));

        verify(saveUserPort, never()).save(any());
    }

    @Test
    void onSellerAddressAdded_whenUserIsSeller_marksAddressRequirementMet() {
        var userId = UUID.randomUUID();
        var seller = user(Role.SELLER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(seller));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new SellerAddressAddedEvent(userId));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().isAddressRequirementMet()).isTrue();
    }

    @Test
    void onBuyerPaymentMethodAttached_marksPaymentRequirementMetForBuyer() {
        var userId = UUID.randomUUID();
        var buyer = user(Role.BUYER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(buyer));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new BuyerPaymentMethodAttachedEvent(userId, UUID.randomUUID()));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().isPaymentRequirementMet()).isTrue();
    }

    @Test
    void onSellerPayoutAccountActivated_marksPaymentRequirementMetForSeller() {
        var userId = UUID.randomUUID();
        var seller = user(Role.SELLER);
        when(loadUserPort.loadById(userId)).thenReturn(Optional.of(seller));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new SellerPayoutAccountActivatedEvent(userId, UUID.randomUUID()));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertThat(captor.getValue().isPaymentRequirementMet()).isTrue();
    }
}
