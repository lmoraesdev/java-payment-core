package com.lmoraesdev.payment.adapter.out.persistence;

import com.lmoraesdev.payment.domain.model.ChargeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "charges")
public class ChargeJpaEntity {
    @Id private UUID id;

    @Column(name = "amount_centavos", nullable = false)
    private Long amountCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChargeJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getAmountCentavos() {
        return amountCentavos;
    }

    public void setAmountCentavos(Long amountCentavos) {
        this.amountCentavos = amountCentavos;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
