package com.livecomerce.store.api;

import jakarta.annotation.Nullable;

import java.util.UUID;

/** {@code categoryId = null} clears the manual override so the inferred category applies again. */
public record SetStoreCategoryRequest(

        @Nullable
        UUID categoryId
) {}
