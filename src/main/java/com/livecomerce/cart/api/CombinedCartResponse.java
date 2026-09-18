package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.CombinedCartView;

import java.util.List;

record CombinedCartResponse(List<StoreCartResponse> stores) {

    static CombinedCartResponse from(CombinedCartView view) {
        return new CombinedCartResponse(view.stores().stream().map(StoreCartResponse::from).toList());
    }
}
