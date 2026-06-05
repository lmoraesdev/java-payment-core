package com.lmoraesdev.payment.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateChargeRequest(@NotNull @Positive BigDecimal amount) {}
