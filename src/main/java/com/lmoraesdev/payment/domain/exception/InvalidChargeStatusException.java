package com.lmoraesdev.payment.domain.exception;

public class InvalidChargeStatusException extends DomainException {

    public InvalidChargeStatusException(String status) {
        super("INVALID_CHARGE_STATUS", "Status de charge inválido: %s".formatted(status));
    }
}
