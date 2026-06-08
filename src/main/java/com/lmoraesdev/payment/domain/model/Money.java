package com.lmoraesdev.payment.domain.model;

import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {
    public Money {
        if (amount == null) {
            throw new InvalidAmountException("amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("amount must be greater than zero");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidAmountException("amount must have at most 2 decimal places");
        }
    }
}
