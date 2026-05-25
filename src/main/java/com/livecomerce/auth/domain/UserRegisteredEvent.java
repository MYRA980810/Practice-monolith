package com.livecomerce.auth.domain;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String contact, Role role) {}
