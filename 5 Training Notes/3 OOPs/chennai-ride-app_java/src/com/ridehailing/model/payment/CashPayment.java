package com.ridehailing.model.payment;

import com.ridehailing.model.common.Money;
import com.ridehailing.model.user.Captain;

/** Cash always succeeds: the customer hands the money to the captain. */
public final class CashPayment implements Payable {

    private final Captain collectedBy;
    private PaymentStatus status = PaymentStatus.NOT_ATTEMPTED;
    private String resultMessage = "not attempted";

    public CashPayment(Captain collectedBy) {
        this.collectedBy = collectedBy;
    }

    @Override
    public PaymentStatus pay(Money amount) {
        if (status != PaymentStatus.NOT_ATTEMPTED) {
            throw new IllegalStateException("A payment object is used once");
        }
        status = PaymentStatus.SUCCESS;
        resultMessage = amount + " collected in cash by Captain " + collectedBy.getName();
        return status;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public PaymentStatus status() {
        return status;
    }

    @Override
    public String resultMessage() {
        return resultMessage;
    }

    public Captain getCollectedBy() {
        return collectedBy;
    }
}
