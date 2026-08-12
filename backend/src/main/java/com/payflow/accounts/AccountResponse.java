package com.payflow.accounts;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String holderName,
        BigDecimal balance,
        String currency
) {
    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getHolderName(),
                account.getBalance(),
                account.getCurrency().getCurrencyCode()
        );
    }
}
