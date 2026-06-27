package com.livecomerce.store.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateStoreRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @URL
        @Size(max = 500)
        String logoUrl,

        @Size(max = 255)
        String shippingStreet,

        @Size(max = 20)
        String shippingExtNumber,

        @Size(max = 20)
        String shippingIntNumber,

        @Size(max = 100)
        String shippingNeighborhood,

        @Size(max = 100)
        String shippingCity,

        @Size(max = 100)
        String shippingState,

        @Size(max = 10)
        String shippingZipCode,

        @Size(max = 3)
        String shippingCountry
) {}
