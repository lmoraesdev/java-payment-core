package com.lmoraesdev.payment.application.port.in;

import java.math.BigDecimal;

public record CreateChargeCommand(BigDecimal amount, String idempotencyKey) {}
