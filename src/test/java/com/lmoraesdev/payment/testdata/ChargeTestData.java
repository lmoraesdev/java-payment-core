package com.lmoraesdev.payment.testdata;

import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.domain.model.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ChargeTestData {

    private Money amount = new Money(new BigDecimal("10.50"));
    private ChargeStatus status = ChargeStatus.ACTIVE;

    private ChargeTestData() {}

    public static ChargeTestData aCharge() {
        return new ChargeTestData();
    }

    public static Money money(String value) {
        return new Money(new BigDecimal(value));
    }

    public ChargeTestData withAmount(String value) {
        this.amount = new Money(new BigDecimal(value));
        return this;
    }

    public ChargeTestData withStatus(ChargeStatus s) {
        this.status = s;
        return this;
    }

    public Charge build() {
        return Charge.restore(UUID.randomUUID(), amount, status, Instant.now());
    }
}
