package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.GetBuyerPaymentMethodsUseCase;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.domain.BuyerPaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBuyerPaymentMethodsService implements GetBuyerPaymentMethodsUseCase {

    private final BuyerPaymentMethodPort buyerPaymentMethodPort;

    @Override
    public List<BuyerPaymentMethod> listByUserId(UUID userId) {
        return buyerPaymentMethodPort.loadByUserId(userId);
    }
}
