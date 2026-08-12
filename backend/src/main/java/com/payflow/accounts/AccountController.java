package com.payflow.accounts;

import com.payflow.shared.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository repository;

    public AccountController(AccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<AccountResponse> list() {
        return repository.findAllByOrderByHolderNameAsc().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{accountId}")
    AccountResponse find(@PathVariable UUID accountId) {
        return repository.findById(accountId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
    }
}
