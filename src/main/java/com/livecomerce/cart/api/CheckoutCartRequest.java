package com.livecomerce.cart.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

record CheckoutCartRequest(
        @NotEmpty @Valid List<SelectedItemRequest> selectedItems
) {}
