package com.payflow.transfers;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.validation.annotation.Validated;

import com.payflow.shared.CorrelationIdFilter;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transferências", description = "Criação e consulta de transferências fictícias")
@Validated
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping("/{id}/reversals")
    @Operation(summary = "Estornar integralmente uma transferência")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estorno criado ou repetição idempotente",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "A operação informada é um estorno",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Transferência não encontrada para o usuário",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transferência já estornada ou chave conflitante",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente para compensar a operação",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<TransferResponse> reverse(
            @PathVariable UUID id,
            @Parameter(description = "UUID único da tentativa", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE_NAME) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        TransferResponse response = service.reverse(
                id,
                UUID.fromString(jwt.getSubject()),
                idempotencyKey,
                correlationId
        );
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/transfers/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping
    @Operation(summary = "Criar uma transferência")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request,
                                            @Parameter(description = "UUID único da tentativa", required = true)
                                            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE_NAME) String correlationId,
                                            @AuthenticationPrincipal Jwt jwt) {
        TransferResponse response = service.create(
                request,
                UUID.fromString(jwt.getSubject()),
                idempotencyKey,
                correlationId
        );
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar transferências")
    @SecurityRequirement(name = "bearerAuth")
    TransferPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @AuthenticationPrincipal Jwt jwt) {
        return service.list(
                UUID.fromString(jwt.getSubject()),
                page,
                size,
                status,
                from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar uma transferência pelo identificador")
    @SecurityRequirement(name = "bearerAuth")
    TransferResponse find(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.find(id, UUID.fromString(jwt.getSubject()));
    }
}
