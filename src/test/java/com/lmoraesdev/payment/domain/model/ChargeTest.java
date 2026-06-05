package com.lmoraesdev.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Charge")
class ChargeTest {

    private final Money amount = new Money(new BigDecimal("10.00"));

    @Test
    @DisplayName("create gera id único e inicia ACTIVE")
    void createGeneratesUniqueIdAndStartsActive() {
        Charge a = Charge.create(amount);
        Charge b = Charge.create(amount);

        assertThat(a.getStatus()).isEqualTo(ChargeStatus.ACTIVE);
        assertThat(a.getId()).isNotNull();
        assertThat(a.getCreatedAt()).isNotNull();
        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    @DisplayName("create rejeita amount nulo")
    void createRejectsNullAmount() {
        assertThatThrownBy(() -> Charge.create(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("restore reconstitui sem gerar novo id ou data")
    void restorePreservesIdAndTimestamp() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");

        Charge charge = Charge.restore(id, amount, ChargeStatus.PAID, createdAt);

        assertThat(charge.getId()).isEqualTo(id);
        assertThat(charge.getStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(charge.getAmount()).isEqualTo(amount);
        assertThat(charge.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("equals e hashCode baseados somente no id")
    void equalityIsIdBased() {
        UUID id = UUID.randomUUID();
        Charge a = Charge.restore(id, amount, ChargeStatus.ACTIVE, Instant.now());
        Charge b =
                Charge.restore(
                        id, new Money(new BigDecimal("99.00")), ChargeStatus.PAID, Instant.now());
        Charge c = Charge.restore(UUID.randomUUID(), amount, ChargeStatus.ACTIVE, Instant.now());

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
