package com.lmoraesdev.payment.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateChargeResponse(UUID id, String status, BigDecimal amount, Instant createdAt) {}
