package com.lmoraesdev.payment.adapter.in.web.documentation;

import com.lmoraesdev.payment.adapter.in.web.WebhookProviderPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Webhooks", description = "Recebimento de eventos do provedor Pix")
@RequestMapping("/webhooks")
public interface WebhookControllerDoc {

    @Operation(
            summary = "Receber webhook do provedor",
            description =
                    "Processa uma notificação de mudança de status da cobrança. Sempre retorna 200,"
                            + " inclusive para eventos duplicados (idempotente por eventId)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Webhook processado (ou já processado anteriormente)"),
        @ApiResponse(
                responseCode = "400",
                description = "Payload inválido — Problem Details com erros por campo",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cobrança não encontrada",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Transição de status inválida para o estado atual da cobrança",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/provider")
    ResponseEntity<Void> receive(@Valid @RequestBody WebhookProviderPayload payload);
}
