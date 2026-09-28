package com.ridehailing.model.payment;

import com.ridehailing.exception.InsufficientWalletBalanceException;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.user.Customer;

/** Pays from the in-app wallet. All or nothing: never a partial deduction. */
public final class WalletPayment implements Payable {

    private final Customer customer;
    private PaymentStatus status = PaymentStatus.NOT_ATTEMPTED;
    private String resultMessage = "not attempted";

    public WalletPayment(Customer customer) {
        this.customer = customer;
    }

    // Overriding may NARROW the checked exception: only InsufficientWalletBalanceException here.
    @Override
    public PaymentStatus pay(Money amount) throws InsufficientWalletBalanceException {
        if (status != PaymentStatus.NOT_ATTEMPTED) {
            throw new IllegalStateException("A payment object is used once");
        }
        try {
            customer.debitWallet(amount);
        } catch (InsufficientWalletBalanceException e) {
            status = PaymentStatus.FAILED;
            resultMessage = "Wallet has only " + e.getBalance() + ", needs " + e.getRequired();
            throw e;
        }
        status = PaymentStatus.SUCCESS;
        resultMessage = amount + " paid from wallet (balance now " + customer.getWalletBalance() + ")";
        return status;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    public PaymentStatus status() {
        return status;
    }

    @Override
    public String resultMessage() {
        return resultMessage;
    }
}
