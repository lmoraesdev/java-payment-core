package com.lmoraesdev.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ChargeCreatedEvent(UUID chargeId, BigDecimal amount, Instant occurredAt) {}
