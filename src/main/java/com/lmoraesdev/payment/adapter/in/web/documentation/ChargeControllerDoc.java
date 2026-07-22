package com.lmoraesdev.payment.adapter.in.web.documentation;

import com.lmoraesdev.payment.adapter.in.web.CreateChargeRequest;
import com.lmoraesdev.payment.adapter.in.web.CreateChargeResponse;
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
}
