package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.AddSellerAddressUseCase;
import com.livecomerce.store.application.port.in.DeleteSellerAddressUseCase;
import com.livecomerce.store.application.port.in.GetSellerAddressesUseCase;
import com.livecomerce.store.application.port.in.SetDefaultSellerAddressUseCase;
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
@RequestMapping("/api/seller/addresses")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
class SellerAddressController {

    private final AddSellerAddressUseCase addSellerAddressUseCase;
    private final GetSellerAddressesUseCase getSellerAddressesUseCase;
    private final SetDefaultSellerAddressUseCase setDefaultSellerAddressUseCase;
    private final DeleteSellerAddressUseCase deleteSellerAddressUseCase;

    @PostMapping
    ResponseEntity<SellerAddressResponse> addAddress(
            @Valid @RequestBody AddSellerAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var address = addSellerAddressUseCase.add(new AddSellerAddressUseCase.AddSellerAddressCommand(
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
                request.longitude()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(SellerAddressResponse.from(address));
    }

    @GetMapping
    ResponseEntity<List<SellerAddressResponse>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {

        var addresses = getSellerAddressesUseCase.listByUserId(principal.getUserId())
                .stream()
                .map(SellerAddressResponse::from)
                .toList();
        return ResponseEntity.ok(addresses);
    }

    @PatchMapping("/{addressId}/default")
    ResponseEntity<Void> setDefault(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {

        setDefaultSellerAddressUseCase.setDefault(principal.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{addressId}")
    ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {

        deleteSellerAddressUseCase.delete(principal.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
