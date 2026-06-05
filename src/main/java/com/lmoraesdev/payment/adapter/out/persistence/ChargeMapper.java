package com.lmoraesdev.payment.adapter.out.persistence;

import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;

final class ChargeMapper {
    private ChargeMapper() {}

    static ChargeJpaEntity toEntity(Charge charge) {
        ChargeJpaEntity entity = new ChargeJpaEntity();

        entity.setId(charge.getId());
        entity.setAmount(charge.getAmount().amount());
        entity.setStatus(charge.getStatus());
        entity.setCreatedAt(charge.getCreatedAt());

        return entity;
    }

    static Charge toDomain(ChargeJpaEntity entity) {

        return Charge.restore(
                entity.getId(),
                new Money(entity.getAmount()),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
