package com.payflow.transfers;

import com.payflow.accounts.Account;
import com.payflow.accounts.AccountRepository;
import com.payflow.shared.BusinessException;
import com.payflow.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional
    public TransferResponse create(CreateTransferRequest request, UUID ownerId) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BusinessException("same-account", "As contas de origem e destino devem ser diferentes.");
        }

        List<Account> lockedAccounts = accountRepository.findAllForUpdate(
                List.of(request.sourceAccountId(), request.destinationAccountId())
        );
        if (lockedAccounts.size() != 2) {
            throw new ResourceNotFoundException("Conta de origem ou destino não encontrada.");
        }

        Map<UUID, Account> byId = lockedAccounts.stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        Account source = byId.get(request.sourceAccountId());
        Account destination = byId.get(request.destinationAccountId());
        if (!source.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Conta de origem ou destino não encontrada.");
        }
        String requestedCurrency = request.currency().toUpperCase();

        if (!source.getCurrency().equals(destination.getCurrency())
                || !source.getCurrency().getCurrencyCode().equals(requestedCurrency)) {
            throw new BusinessException("currency-mismatch", "A moeda deve ser igual nas duas contas e na transferência.");
        }

        var amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        source.debit(amount);
        destination.credit(amount);

        Transfer transfer = Transfer.completed(source.getId(), destination.getId(), amount, requestedCurrency);
        return TransferResponse.from(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> list(UUID ownerId) {
        return transferRepository.findVisibleTo(ownerId).stream().map(TransferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse find(UUID id, UUID ownerId) {
        return transferRepository.findVisibleById(id, ownerId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada."));
    }
}
