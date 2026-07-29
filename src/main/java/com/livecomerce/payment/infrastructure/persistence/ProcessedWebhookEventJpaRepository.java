package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProcessedWebhookEventJpaRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {
}
