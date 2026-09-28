package com.ridehailing.model.payment;

/** How the customer chooses to pay. Chosen at booking, can be switched at the end of the ride. */
public enum PaymentMethod {

    UPI("UPI"),
    CASH("Cash"),
    WALLET("Rapido Wallet");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
