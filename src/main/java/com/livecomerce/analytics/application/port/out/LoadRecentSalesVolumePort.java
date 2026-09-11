package com.livecomerce.analytics.application.port.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadRecentSalesVolumePort {

    Map<UUID, BigDecimal> loadRecentSalesVolume(Collection<UUID> storeIds, OffsetDateTime from, OffsetDateTime to);
}
