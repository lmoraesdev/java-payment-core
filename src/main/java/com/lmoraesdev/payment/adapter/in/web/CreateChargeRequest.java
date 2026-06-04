package com.lmoraesdev.payment.adapter.in.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateChargeRequest(@NotNull @Positive BigDecimal amount) {
}
