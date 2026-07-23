package com.lmoraesdev.payment.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@Schema(description = "Payload de webhook recebido do provedor Pix")
public record WebhookProviderPayload(
        @Schema(description = "Identificador único do evento, usado para deduplicação") @NotBlank
                String eventId,
        @Schema(description = "Identificador da cobrança") @NotNull UUID chargeId,
        @Schema(description = "Novo status da cobrança", example = "PAID")
                @NotBlank
                @Pattern(
                        regexp = "PAID|EXPIRED|CANCELLED",
                        message = "status deve ser PAID, EXPIRED ou CANCELLED")
                String status) {}
