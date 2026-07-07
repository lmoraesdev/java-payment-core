package com.lmoraesdev.payment.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "Requisição para criar uma cobrança Pix")
public record CreateChargeRequest(
        @Schema(description = "Valor da cobrança em reais", example = "150.00", minimum = "0.01")
                @NotNull
                @Positive
                BigDecimal amount) {}
