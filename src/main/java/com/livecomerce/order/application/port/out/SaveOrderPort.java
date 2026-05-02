package com.livecomerce.order.application.port.out;

import com.livecomerce.order.domain.Order;

public interface SaveOrderPort {

    Order save(Order order);
}
