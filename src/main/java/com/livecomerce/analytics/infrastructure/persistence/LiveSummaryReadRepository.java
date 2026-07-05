package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.LiveReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Read-only access to the shared {@code lives} table via {@link LiveReadEntity},
 * used to build the live-end summary snapshot (startedAt/endedAt/peakViewers/storeId).
 */
interface LiveSummaryReadRepository extends JpaRepository<LiveReadEntity, UUID> {
}
