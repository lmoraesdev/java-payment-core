package com.lmoraesdev.payment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Charge {
    private final UUID id;
    private final Money amount;
    private ChargeStatus status;
    private final Instant createdAt;

    private Charge(UUID id, Money amount, ChargeStatus status, Instant createdAt) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Charge create(Money amount) {
        Objects.requireNonNull(amount, "O montante (Money) é obrigatório");

        return new Charge(UUID.randomUUID(), amount, ChargeStatus.ACTIVE, Instant.now());
    }

    public static Charge restore(UUID id, Money amount, ChargeStatus status, Instant createdAt) {
        Objects.requireNonNull(id, "O id é obrigatório");
        Objects.requireNonNull(amount, "O montante (Money) é obrigatório");
        Objects.requireNonNull(status, "O status é obrigatório");
        Objects.requireNonNull(createdAt, "A data de criação é obrigatória");

        return new Charge(id, amount, status, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Money getAmount() {
        return amount;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Charge other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
