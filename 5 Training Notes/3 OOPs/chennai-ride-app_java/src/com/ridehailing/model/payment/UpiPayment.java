package com.ridehailing.model.payment;

import com.ridehailing.model.common.Money;

import java.util.Random;

/**
 * UPI can fail when the customer's bank is down. The outage schedule is fixed, and the
 * transaction reference comes from a seeded Random, so every run prints the same output.
 */
public final class UpiPayment implements Payable {

    private final String upiId;
    private final boolean bankReachable;
    private final Random referenceGenerator;
    private PaymentStatus status = PaymentStatus.NOT_ATTEMPTED;
    private String resultMessage = "not attempted";

    public UpiPayment(String upiId, boolean bankReachable, Random referenceGenerator) {
        this.upiId = upiId;
        this.bankReachable = bankReachable;
        this.referenceGenerator = referenceGenerator;
    }

    @Override
    public PaymentStatus pay(Money amount) {
        if (status != PaymentStatus.NOT_ATTEMPTED) {
            throw new IllegalStateException("A payment object is used once; create a new one to retry");
        }
        if (!bankReachable) {
            status = PaymentStatus.FAILED;
            resultMessage = "UPI to " + upiId + " failed: bank server not responding";
            return status;
        }
        long reference = 400000000000L + (long) (referenceGenerator.nextDouble() * 99999999999L);
        status = PaymentStatus.SUCCESS;
        resultMessage = amount + " paid from " + upiId + " (UPI ref " + reference + ")";
        return status;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
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
