package com.lmoraesdev.payment.adapter.out.persistence.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_webhook_events_event_id", columnNames = "event_id"))
public class WebhookEventEntity {

    @Id private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "charge_id", nullable = false, updatable = false)
    private UUID chargeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WebhookEventEntity() {
        // JPA
    }

    public static WebhookEventEntity create(String eventId, UUID chargeId) {
        WebhookEventEntity entity = new WebhookEventEntity();
        entity.id = UUID.randomUUID();
        entity.eventId = eventId;
        entity.chargeId = chargeId;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
