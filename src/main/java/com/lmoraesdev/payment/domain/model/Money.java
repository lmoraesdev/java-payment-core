package com.lmoraesdev.payment.domain.model;

import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
import java.math.BigDecimal;

public record Money(BigDecimal amount) {
    public Money {
        if (amount == null) {
            throw new InvalidAmountException("amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("amount must be greater than zero");
        }
    }
}
