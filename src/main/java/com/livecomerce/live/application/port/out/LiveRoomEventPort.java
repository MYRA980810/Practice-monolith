package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.Live;

/**
 * Broadcasts a {@link RoomEvent} to everyone in a live's room.
 *
 * <p>Implementations own failure handling: they never throw. A delivery failure
 * is logged/metered by the implementation and reported as {@code false}.
 * A deliberate skip (e.g. the live has no room on the active provider, so
 * retrying would be pointless) returns {@code true}.
 *
 * @return {@code true} if the event was handed to the provider or deliberately
 *         skipped; {@code false} only when delivery was attempted and failed
 */
public interface LiveRoomEventPort {

    boolean publish(Live live, RoomEvent event);
}
