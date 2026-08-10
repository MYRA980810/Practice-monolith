package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.AddBuyerAddressUseCase;
import com.livecomerce.store.application.port.in.DeleteBuyerAddressUseCase;
import com.livecomerce.store.application.port.in.GetBuyerAddressesUseCase;
import com.livecomerce.store.application.port.in.SetDefaultBuyerAddressUseCase;
import com.livecomerce.store.application.port.in.UpdateBuyerAddressUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/addresses")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
class BuyerAddressController {

    private final AddBuyerAddressUseCase addBuyerAddressUseCase;
    private final UpdateBuyerAddressUseCase updateBuyerAddressUseCase;
    private final GetBuyerAddressesUseCase getBuyerAddressesUseCase;
    private final SetDefaultBuyerAddressUseCase setDefaultBuyerAddressUseCase;
    private final DeleteBuyerAddressUseCase deleteBuyerAddressUseCase;

    @PostMapping
    ResponseEntity<BuyerAddressResponse> addAddress(
            @Valid @RequestBody AddBuyerAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var address = addBuyerAddressUseCase.add(new AddBuyerAddressUseCase.AddBuyerAddressCommand(
                principal.getUserId(),
                request.street(),
                request.extNumber(),
                request.intNumber(),
                request.neighborhood(),
                request.city(),
                request.state(),
                request.zipCode(),
                request.country(),
                request.isDefault(),
                request.latitude(),
                request.longitude(),
                request.addressType()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(BuyerAddressResponse.from(address));
    }

    @PutMapping("/{addressId}")
    ResponseEntity<BuyerAddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateBuyerAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var address = updateBuyerAddressUseCase.update(new UpdateBuyerAddressUseCase.UpdateBuyerAddressCommand(
                principal.getUserId(), addressId,
                request.street(), request.extNumber(), request.intNumber(), request.neighborhood(),
                request.city(), request.state(), request.zipCode(), request.country(),
                request.latitude(), request.longitude(), request.addressType()
        ));
        return ResponseEntity.ok(BuyerAddressResponse.from(address));
    }

    @GetMapping
    ResponseEntity<List<BuyerAddressResponse>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {

        var addresses = getBuyerAddressesUseCase.listByUserId(principal.getUserId())
                .stream()
                .map(BuyerAddressResponse::from)
                .toList();
        return ResponseEntity.ok(addresses);
    }

    @PatchMapping("/{addressId}/default")
    ResponseEntity<Void> setDefault(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {

        setDefaultBuyerAddressUseCase.setDefault(principal.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{addressId}")
    ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {

        deleteBuyerAddressUseCase.delete(principal.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
