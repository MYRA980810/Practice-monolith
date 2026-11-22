package com.livecomerce.live.domain;

/**
 * The chat provider refused the call for a reason only an operator can fix (IVS AccessDeniedException,
 * e.g. a missing IAM permission, or PendingVerificationException): not retryable. The message names
 * the action for the logs; the HTTP response uses a generic body.
 */
public class ChatProviderMisconfiguredException extends RuntimeException {

    public ChatProviderMisconfiguredException(String action, Throwable cause) {
        super("Chat provider refused " + action + ": check the IAM permissions and the account status", cause);
    }
}
