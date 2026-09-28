package com.ridehailing.exception;

import com.ridehailing.model.common.Money;

/** Wallet balance is lower than the amount due. The wallet is never debited partially. */
public class InsufficientWalletBalanceException extends RideHailingException {

    private final Money balance;
    private final Money required;

    public InsufficientWalletBalanceException(Money balance, Money required) {
        super("Wallet balance " + balance + " is less than the amount due " + required);
        this.balance = balance;
        this.required = required;
    }

    public Money getBalance() {
        return balance;
    }

    public Money getRequired() {
        return required;
    }
}
