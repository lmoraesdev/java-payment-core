package com.lmoraesdev.payment.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GetChargeResult(UUID id, String status, BigDecimal amount, Instant createdAt) {}
