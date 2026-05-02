package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

public interface TokenGeneratorPort {
    String generate(User user);
}
