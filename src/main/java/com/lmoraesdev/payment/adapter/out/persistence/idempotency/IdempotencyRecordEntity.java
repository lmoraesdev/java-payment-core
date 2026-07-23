package com.lmoraesdev.payment.adapter.out.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_idempotency_records_key",
                        columnNames = "idempotency_key"))
public class IdempotencyRecordEntity {

    @Id private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String key;

    @Column(name = "request_hash", nullable = false, updatable = false)
    private String requestHash;

    @Column(name = "charge_id", nullable = false, updatable = false)
    private UUID chargeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecordEntity() {
        // JPA
    }

    public static IdempotencyRecordEntity create(
            String key, String requestHash, UUID chargeId, String responseBodyJson) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.id = UUID.randomUUID();
        entity.key = key;
        entity.requestHash = requestHash;
        entity.chargeId = chargeId;
        entity.responseBody = responseBodyJson;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
