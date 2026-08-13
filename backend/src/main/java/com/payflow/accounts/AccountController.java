package com.payflow.accounts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Contas", description = "Consulta de contas fictícias e seus saldos")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar contas")
    @SecurityRequirement(name = "bearerAuth")
    List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Consultar uma conta pelo identificador")
    @SecurityRequirement(name = "bearerAuth")
    AccountResponse find(@PathVariable UUID accountId, @AuthenticationPrincipal Jwt jwt) {
        return service.find(accountId, UUID.fromString(jwt.getSubject()));
    }
}
