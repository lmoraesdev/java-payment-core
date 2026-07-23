package com.lmoraesdev.payment.adapter.out.persistence.webhook;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventJpaRepository extends JpaRepository<WebhookEventEntity, UUID> {

    boolean existsByEventId(String eventId);
}
