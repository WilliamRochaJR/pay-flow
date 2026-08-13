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
                     UUID ownerId, UUID idempotencyKey) {
        this.id = UUID.randomUUID();
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = TransferStatus.COMPLETED;
        this.ownerId = ownerId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public static Transfer completed(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                                     String currency, UUID ownerId, UUID idempotencyKey) {
        return new Transfer(sourceAccountId, destinationAccountId, amount, currency, ownerId, idempotencyKey);
    }

    public UUID getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
}
