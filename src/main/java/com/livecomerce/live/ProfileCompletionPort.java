package com.livecomerce.live;

import java.util.UUID;

public interface ProfileCompletionPort {
    boolean isProfileComplete(UUID userId);
}
