package com.lmoraesdev.payment.domain.exception;

public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String key) {
        super(
                "IDEMPOTENCY_CONFLICT",
                "Idempotency-Key já utilizada com um corpo de requisição diferente: %s"
                        .formatted(key));
    }
}
