package com.lmoraesdev.payment.adapter.out.persistence.outbox;

public enum OutboxStatus {
    PENDING,
    IN_FLIGHT,
    PUBLISHED
}
