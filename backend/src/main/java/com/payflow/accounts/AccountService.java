package com.payflow.accounts;

import com.payflow.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByHolderNameAsc(ownerId).stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse find(UUID accountId, UUID ownerId) {
        return repository.findByIdAndOwnerId(accountId, ownerId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
    }
}
