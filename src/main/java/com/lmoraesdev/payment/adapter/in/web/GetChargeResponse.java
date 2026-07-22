package com.lmoraesdev.payment.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Cobrança Pix")
public record GetChargeResponse(
        @Schema(description = "Identificador único da cobrança") UUID id,
        @Schema(description = "Status da cobrança", example = "ACTIVE") String status,
        @Schema(description = "Valor da cobrança em reais", example = "150.00") BigDecimal amount,
        @Schema(description = "Data e hora de criação (UTC)") Instant createdAt) {}
