package com.livecomerce.auth.api;

import com.livecomerce.auth.api.validation.ValidContact;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@ValidContact @NotBlank String contact) {}
