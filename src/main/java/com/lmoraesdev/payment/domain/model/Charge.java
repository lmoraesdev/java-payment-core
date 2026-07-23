package com.lmoraesdev.payment.domain.model;

import com.lmoraesdev.payment.domain.exception.InvalidStateTransitionException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Charge {

    private static final Duration EXPIRATION = Duration.ofMinutes(30);

    private static final Map<ChargeStatus, Set<ChargeStatus>> VALID_TRANSITIONS =
            Map.of(
                    ChargeStatus.ACTIVE,
                    Set.of(ChargeStatus.PAID, ChargeStatus.EXPIRED, ChargeStatus.CANCELLED));

    private final UUID id;
    private final Money amount;
    private ChargeStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Long version;

    private Charge(
            UUID id,
            Money amount,
            ChargeStatus status,
            Instant createdAt,
            Instant expiresAt,
            Long version) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.version = version;
    }

    public static Charge create(Money amount) {
        Objects.requireNonNull(amount, "O montante (Money) é obrigatório");

        Instant createdAt = Instant.now();
        return new Charge(
                UUID.randomUUID(),
                amount,
                ChargeStatus.ACTIVE,
                createdAt,
                createdAt.plus(EXPIRATION),
                null);
    }

    public static Charge restore(
            UUID id,
            Money amount,
            ChargeStatus status,
            Instant createdAt,
            Instant expiresAt,
            Long version) {
        Objects.requireNonNull(id, "O id é obrigatório");
        Objects.requireNonNull(amount, "O montante (Money) é obrigatório");
        Objects.requireNonNull(status, "O status é obrigatório");
        Objects.requireNonNull(createdAt, "A data de criação é obrigatória");
        Objects.requireNonNull(expiresAt, "A data de expiração é obrigatória");

        return new Charge(id, amount, status, createdAt, expiresAt, version);
    }

    public void transitionTo(ChargeStatus newStatus) {
        Set<ChargeStatus> allowed = VALID_TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(status, newStatus);
        }
        this.status = newStatus;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Long getVersion() {
        return version;
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
