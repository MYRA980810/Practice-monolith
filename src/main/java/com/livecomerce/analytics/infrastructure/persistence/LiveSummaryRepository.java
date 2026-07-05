package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.LiveSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface LiveSummaryRepository extends JpaRepository<LiveSummary, UUID> {
}
