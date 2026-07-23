package com.lmoraesdev.payment.adapter.out.persistence;

import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;
import java.math.BigDecimal;

final class ChargeMapper {

    private ChargeMapper() {}

    static ChargeJpaEntity toEntity(Charge charge) {
        ChargeJpaEntity entity = new ChargeJpaEntity();

        entity.setId(charge.getId());
        entity.setAmountCentavos(charge.getAmount().amount().movePointRight(2).longValueExact());
        entity.setStatus(charge.getStatus());
        entity.setCreatedAt(charge.getCreatedAt());
        entity.setExpiresAt(charge.getExpiresAt());
        entity.setVersion(charge.getVersion());

        return entity;
    }

    static Charge toDomain(ChargeJpaEntity entity) {
        return Charge.restore(
                entity.getId(),
                new Money(BigDecimal.valueOf(entity.getAmountCentavos(), 2)),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getVersion());
    }
}
