package com.payflow.transfers;

import com.payflow.accounts.Account;
import com.payflow.accounts.AccountRepository;
import com.payflow.shared.BusinessException;
import com.payflow.shared.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final EntityManager entityManager;
    private final TransferMetrics metrics;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository,
                           EntityManager entityManager, TransferMetrics metrics) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.entityManager = entityManager;
        this.metrics = metrics;
    }

    @Transactional
    public TransferResponse create(CreateTransferRequest request, UUID ownerId, UUID idempotencyKey) {
        long startedAtNanos = System.nanoTime();
        String outcome = "failed";
        try {
            lockIdempotencyKey(ownerId, idempotencyKey);
            var amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
            String requestedCurrency = request.currency().toUpperCase();
            var previous = transferRepository.findByOwnerIdAndIdempotencyKey(ownerId, idempotencyKey);
            if (previous.isPresent()) {
                TransferResponse response = replay(previous.get(), request, amount, requestedCurrency);
                outcome = "replayed";
                metrics.replayedAfterCommit();
                return response;
            }

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
            if (!source.getCurrency().equals(destination.getCurrency())
                    || !source.getCurrency().getCurrencyCode().equals(requestedCurrency)) {
                throw new BusinessException("currency-mismatch", "A moeda deve ser igual nas duas contas e na transferência.");
            }

            source.debit(amount);
            destination.credit(amount);

            Transfer transfer = Transfer.completed(source.getId(), destination.getId(), amount, requestedCurrency,
                    ownerId, idempotencyKey);
            TransferResponse response = TransferResponse.from(transferRepository.save(transfer));
            outcome = "completed";
            metrics.completedAfterCommit();
            return response;
        } catch (BusinessException exception) {
            outcome = "rejected";
            metrics.rejected(exception.getCode());
            throw exception;
        } catch (ResourceNotFoundException exception) {
            outcome = "rejected";
            metrics.rejected("not-found");
            throw exception;
        } catch (RuntimeException exception) {
            metrics.failed();
            throw exception;
        } finally {
            metrics.recordDuration(startedAtNanos, outcome);
        }
    }

    private void lockIdempotencyKey(UUID ownerId, UUID idempotencyKey) {
        entityManager.createNativeQuery("""
                        SELECT pg_advisory_xact_lock(
                            hashtextextended(CAST(?1 AS text) || ':' || CAST(?2 AS text), 0)
                        )
                        """)
                .setParameter(1, ownerId)
                .setParameter(2, idempotencyKey)
                .getSingleResult();
    }

    private TransferResponse replay(Transfer transfer, CreateTransferRequest request, java.math.BigDecimal amount,
                                    String currency) {
        boolean sameRequest = transfer.getSourceAccountId().equals(request.sourceAccountId())
                && transfer.getDestinationAccountId().equals(request.destinationAccountId())
                && transfer.getAmount().compareTo(amount) == 0
                && transfer.getCurrency().equals(currency);
        if (!sameRequest) {
            throw new BusinessException("idempotency-conflict",
                    "A Idempotency-Key já foi utilizada com dados diferentes.");
        }
        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferPageResponse list(UUID ownerId, int page, int size, TransferStatus status,
                                     Instant fromInstant, Instant toInstant) {
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new BusinessException("invalid-period", "A data inicial não pode ser posterior à data final.");
        }
        var pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );
        var transfers = transferRepository.findAll(visibleTo(ownerId, status, fromInstant, toInstant), pageable);
        return TransferPageResponse.from(transfers);
    }

    private Specification<Transfer> visibleTo(UUID ownerId, TransferStatus status,
                                               Instant fromInstant, Instant toInstant) {
        return (root, query, criteria) -> {
            var sourceAccounts = query.subquery(UUID.class);
            var sourceAccount = sourceAccounts.from(Account.class);
            sourceAccounts.select(sourceAccount.get("id"))
                    .where(criteria.equal(sourceAccount.get("ownerId"), ownerId));

            var destinationAccounts = query.subquery(UUID.class);
            var destinationAccount = destinationAccounts.from(Account.class);
            destinationAccounts.select(destinationAccount.get("id"))
                    .where(criteria.equal(destinationAccount.get("ownerId"), ownerId));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.or(
                    root.get("sourceAccountId").in(sourceAccounts),
                    root.get("destinationAccountId").in(destinationAccounts)
            ));
            if (status != null) {
                predicates.add(criteria.equal(root.get("status"), status));
            }
            if (fromInstant != null) {
                predicates.add(criteria.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(criteria.lessThanOrEqualTo(root.get("createdAt"), toInstant));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public TransferResponse find(UUID id, UUID ownerId) {
        return transferRepository.findVisibleById(id, ownerId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada."));
    }
}
