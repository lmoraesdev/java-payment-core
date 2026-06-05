package com.lmoraesdev.payment.domain.exception;

public class InvalidAmountException extends DomainException {

    public InvalidAmountException(String message) {
        super("INVALID_AMOUNT", message);
    }
}
