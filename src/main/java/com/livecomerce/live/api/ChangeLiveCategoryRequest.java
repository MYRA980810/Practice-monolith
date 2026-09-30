package com.livecomerce.live.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeLiveCategoryRequest(@NotNull UUID categoryId) {}
