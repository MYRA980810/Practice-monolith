package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.User;

public interface SaveUserPort {
    User save(User user);
}
