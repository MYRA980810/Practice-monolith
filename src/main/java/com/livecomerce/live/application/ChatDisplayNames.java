package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Single source of the chat display name, so every chat surface (the IVS token attribute, the
 * seller's ban list) shows exactly the same name for a user. The name is cosmetic: a failed
 * lookup never fails the caller, it falls back to {@value #FALLBACK}.
 */
final class ChatDisplayNames {

    private static final Logger log = LoggerFactory.getLogger(ChatDisplayNames.class);

    static final String FALLBACK = "User";

    /** IVS caps all token attributes at 1 KB in total, so the free-text name is bounded. */
    static final int MAX_CODE_POINTS = 64;

    private ChatDisplayNames() {}

    /**
     * Resolves the display names of {@code userIds} with ONE batch lookup. Every requested user
     * gets an entry; unknown users and a failed lookup get the fallback name.
     */
    static Map<UUID, String> resolve(LoadSellerNamesPort loadUserNamesPort, Collection<UUID> userIds) {
        var ids = new LinkedHashSet<>(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> raw;
        try {
            raw = loadUserNamesPort.loadNames(ids);
        } catch (RuntimeException e) {
            log.warn("Display name lookup failed for {} user(s), using fallback", ids.size(), e);
            raw = Map.of();
        }
        var names = new LinkedHashMap<UUID, String>();
        for (var id : ids) {
            names.put(id, normalize(raw == null ? null : raw.get(id)));
        }
        return names;
    }

    /** Fallback for a missing or blank name; otherwise stripped and truncated to 64 code points. */
    static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return FALLBACK;
        }
        name = name.strip();
        if (name.codePointCount(0, name.length()) > MAX_CODE_POINTS) {
            name = name.substring(0, name.offsetByCodePoints(0, MAX_CODE_POINTS));
        }
        return name;
    }
}
