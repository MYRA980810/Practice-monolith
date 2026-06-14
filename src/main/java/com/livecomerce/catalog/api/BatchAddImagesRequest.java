package com.livecomerce.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

record BatchAddImagesRequest(
        @NotEmpty @Valid List<ProductImageRequest> images
) {}
