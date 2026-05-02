package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.UUID;

public interface ConfirmItemPaymentUseCase {

    Order confirmPayment(ConfirmItemPaymentCommand command);

    record ConfirmItemPaymentCommand(UUID orderId, UUID itemId) {}
}
