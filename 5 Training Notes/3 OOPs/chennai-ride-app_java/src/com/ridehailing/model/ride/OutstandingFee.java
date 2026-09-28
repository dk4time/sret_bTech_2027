package com.ridehailing.model.ride;

import com.ridehailing.model.common.Money;
import com.ridehailing.model.user.Captain;

/**
 * A cancellation or no-show fee the customer owes. It is added to the customer's NEXT paid
 * ride as "Previous cancellation fee", and the money goes to the captain who lost their time
 * (minus platform commission) - not to the captain of the next ride.
 *
 * Immutable value-like object.
 */
public final class OutstandingFee {

    private final Money amount;
    private final Captain owedTo;
    private final String fromRideId;
    private final String reason;

    public OutstandingFee(Money amount, Captain owedTo, String fromRideId, String reason) {
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("A fee must be a positive amount");
        }
        this.amount = amount;
        this.owedTo = owedTo;
        this.fromRideId = fromRideId;
        this.reason = reason;
    }

    public Money getAmount() {
        return amount;
    }

    public Captain getOwedTo() {
        return owedTo;
    }

    public String getFromRideId() {
        return fromRideId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return amount + " (" + reason + ", " + fromRideId + ", to " + owedTo.getName() + ")";
    }
}
