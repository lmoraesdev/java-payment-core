package com.lmoraesdev.payment.adapter.in.web.documentation;

import com.lmoraesdev.payment.adapter.in.web.CreateChargeRequest;
import com.lmoraesdev.payment.adapter.in.web.CreateChargeResponse;
import com.lmoraesdev.payment.adapter.in.web.GetChargeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Charges", description = "Gerenciamento de cobranças Pix")
@RequestMapping("/charges")
public interface ChargeControllerDoc {

    @Operation(
            summary = "Criar cobrança",
            description = "Cria uma nova cobrança Pix com status ACTIVE")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Cobrança criada com sucesso",
                content = @Content(schema = @Schema(implementation = CreateChargeResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Dados inválidos — Problem Details com erros por campo",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Regra de negócio violada",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Erro interno inesperado",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<CreateChargeResponse> create(@Valid @RequestBody CreateChargeRequest request);

    @Operation(summary = "Consultar cobrança", description = "Busca uma cobrança Pix pelo id")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Cobrança encontrada",
                content = @Content(schema = @Schema(implementation = GetChargeResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cobrança não encontrada",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<GetChargeResponse> get(@PathVariable UUID id);
}
