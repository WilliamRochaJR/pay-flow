package com.payflow.transfers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sourceAccountId;

    @Column(nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferType type;

    @Column(updatable = false)
    private UUID originalTransferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @Column(updatable = false)
    private UUID idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    private Transfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency,
                     TransferType type, UUID originalTransferId, UUID ownerId, UUID idempotencyKey) {
        this.id = UUID.randomUUID();
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.originalTransferId = originalTransferId;
        this.status = TransferStatus.COMPLETED;
        this.ownerId = ownerId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public static Transfer completed(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                                     String currency, UUID ownerId, UUID idempotencyKey) {
        return new Transfer(sourceAccountId, destinationAccountId, amount, currency,
                TransferType.INTERNAL_TRANSFER, null, ownerId, idempotencyKey);
    }

    public static Transfer reversalOf(Transfer original, UUID ownerId, UUID idempotencyKey) {
        return new Transfer(
                original.destinationAccountId,
                original.sourceAccountId,
                original.amount,
                original.currency,
                TransferType.REVERSAL,
                original.id,
                ownerId,
                idempotencyKey
        );
    }

    public UUID getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferType getType() { return type; }
    public UUID getOriginalTransferId() { return originalTransferId; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
}
