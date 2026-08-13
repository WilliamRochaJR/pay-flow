package com.payflow.transfers;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transferências", description = "Criação e consulta de transferências fictícias")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Criar uma transferência")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request,
                                            @Parameter(description = "UUID único da tentativa", required = true)
                                            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                            @AuthenticationPrincipal Jwt jwt) {
        TransferResponse response = service.create(request, UUID.fromString(jwt.getSubject()), idempotencyKey);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar transferências")
    @SecurityRequirement(name = "bearerAuth")
    List<TransferResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar uma transferência pelo identificador")
    @SecurityRequirement(name = "bearerAuth")
    TransferResponse find(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.find(id, UUID.fromString(jwt.getSubject()));
    }
}
