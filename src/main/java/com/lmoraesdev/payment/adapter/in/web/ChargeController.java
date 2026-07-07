package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Charges", description = "Gerenciamento de cobranças Pix")
@RestController
@RequestMapping("/charges")
public class ChargeController {
    private final CreateCharge createCharge;

    public ChargeController(CreateCharge createCharge) {
        this.createCharge = createCharge;
    }

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
    public ResponseEntity<CreateChargeResponse> create(
            @Valid @RequestBody CreateChargeRequest request) {
        CreateChargeResult result = createCharge.create(new CreateChargeCommand(request.amount()));

        CreateChargeResponse response =
                new CreateChargeResponse(
                        result.id(), result.status(), result.amount(), result.createdAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
