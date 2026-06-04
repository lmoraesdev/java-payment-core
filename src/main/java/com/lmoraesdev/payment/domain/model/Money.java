package com.lmoraesdev.payment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) {
    public Money{
        Objects.requireNonNull(amount, "O valor (amount) não pode ser nulo");

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor monetário deve ser estritamente maior que zero");
        }
    }
}

