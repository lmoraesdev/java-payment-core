package com.lmoraesdev.payment.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ChargeStatusChangedEvent(UUID chargeId, String from, String to, Instant occurredAt) {}
