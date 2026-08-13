package com.payflow.accounts;

import com.payflow.shared.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    private long version;

    protected Account() {
    }

    private Account(UUID ownerId, String holderName, String currency, BigDecimal balance) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.holderName = holderName;
        this.currency = currency;
        this.balance = balance;
    }

    public static Account demo(UUID ownerId, String holderName, BigDecimal balance) {
        return new Account(ownerId, holderName, "BRL", balance);
    }

    public UUID getId() {
        return id;
    }

    public String getHolderName() {
        return holderName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Currency getCurrency() {
        return Currency.getInstance(currency);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("insufficient-balance", "Saldo insuficiente para realizar a transferência.");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
