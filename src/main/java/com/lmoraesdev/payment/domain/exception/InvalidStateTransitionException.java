package com.lmoraesdev.payment.domain.exception;

import com.lmoraesdev.payment.domain.model.ChargeStatus;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(ChargeStatus from, ChargeStatus to) {
        super(
                "INVALID_STATE_TRANSITION",
                "Não é possível transicionar de %s para %s".formatted(from, to));
    }
}
