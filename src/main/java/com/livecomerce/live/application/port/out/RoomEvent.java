package com.livecomerce.live.application.port.out;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real-time event broadcast to a live's room. {@link #type()} is the wire event
 * name and {@link #attributes()} its fields, in wire order; both keep the names
 * the frontend already consumes.
 */
public sealed interface RoomEvent {

    String type();

    Map<String, Object> attributes();

    record ProductPinned(UUID liveProductId, String productName, BigDecimal price, int displayDurationSeconds)
            implements RoomEvent {
        public String type() { return "product-pinned"; }

        public Map<String, Object> attributes() {
            return fields("liveProductId", liveProductId, "productName", productName,
                    "price", price, "displayDurationSeconds", displayDurationSeconds);
        }
    }

    record ProductExpired(UUID liveProductId, String status) implements RoomEvent {
        public String type() { return "product-expired"; }

        public Map<String, Object> attributes() {
            return fields("liveProductId", liveProductId, "status", status);
        }
    }

    record StockUpdate(UUID liveProductId, int stockRemaining) implements RoomEvent {
        public String type() { return "stock-update"; }

        public Map<String, Object> attributes() {
            return fields("liveProductId", liveProductId, "stockRemaining", stockRemaining);
        }
    }

    record ViewerCount(long count) implements RoomEvent {
        public String type() { return "viewer-count"; }

        public Map<String, Object> attributes() {
            return fields("count", count);
        }
    }

    record LiveReconnecting(UUID liveId, String reason) implements RoomEvent {
        public String type() { return "live-reconnecting"; }

        public Map<String, Object> attributes() {
            return fields("liveId", liveId, "reason", reason);
        }
    }

    record LiveEnded(UUID liveId) implements RoomEvent {
        public String type() { return "live-ended"; }

        public Map<String, Object> attributes() {
            return fields("liveId", liveId);
        }
    }

    private static Map<String, Object> fields(Object... keysAndValues) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
