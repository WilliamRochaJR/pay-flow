package com.payflow.accounts;

import com.payflow.shared.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final AccountRepository repository;

    public AccountController(AccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar contas")
    List<AccountResponse> list() {
        return repository.findAllByOrderByHolderNameAsc().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Consultar uma conta pelo identificador")
    AccountResponse find(@PathVariable UUID accountId) {
        return repository.findById(accountId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
    }
}
