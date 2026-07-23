package com.lmoraesdev.payment.domain.exception;

import java.util.UUID;

public class ChargeNotFoundException extends DomainException {

    public ChargeNotFoundException(UUID id) {
        super("CHARGE_NOT_FOUND", "Charge não encontrada: %s".formatted(id));
    }
}
