package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;

public record ExchangeCodeRequest(@NotBlank String code) {
}
