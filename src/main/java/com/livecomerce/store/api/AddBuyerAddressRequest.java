package com.livecomerce.store.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddBuyerAddressRequest(

        @NotBlank @Size(max = 255)
        String street,

        @Size(max = 20)
        String extNumber,

        @Size(max = 20)
        String intNumber,

        @Size(max = 100)
        String neighborhood,

        @NotBlank @Size(max = 100)
        String city,

        @NotBlank @Size(max = 100)
        String state,

        @NotBlank @Size(max = 10)
        String zipCode,

        @NotBlank @Size(max = 3)
        String country,

        boolean isDefault,

        Double latitude,

        Double longitude
) {}
