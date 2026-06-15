package com.livecomerce.auth.application.port.out;

import java.util.UUID;

public interface IssueRefreshTokenPort {

    /**
     * Issues a new refresh token (new family) for the given user.
     *
     * @return the raw (unhashed) token value to be sent to the client
     */
    String issueForUser(UUID userId);
}
