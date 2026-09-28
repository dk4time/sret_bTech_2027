package com.ridehailing.exception;

/** The customer must first pay for an earlier ride that is PAYMENT_PENDING. */
public class PaymentPendingException extends InvalidBookingException {

    public PaymentPendingException(String message) {
        super(message);
    }
}
