package com.livecomerce.live.application;

/**
 * Single source of truth for the {@code live.chat-provider} flag. IVS Chat is
 * used iff the value is exactly {@code "ivs"} (case-sensitive); anything else,
 * including unset, empty or a typo, means Agora.
 */
public final class ChatProvider {

    public static final String PROPERTY = "live.chat-provider";
    public static final String DEFAULT  = "agora";

    private static final String IVS = "ivs";

    private ChatProvider() {
    }

    public static boolean isIvs(String value) {
        return IVS.equals(value);
    }
}
