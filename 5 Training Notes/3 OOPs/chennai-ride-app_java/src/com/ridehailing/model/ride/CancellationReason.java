package com.ridehailing.model.ride;

/** Why a ride was cancelled, and which party is allowed to use that reason. */
public enum CancellationReason {

    CUSTOMER_CHANGED_PLANS(Party.CUSTOMER, "Customer changed plans"),
    CUSTOMER_NO_SHOW(Party.CAPTAIN, "Customer did not show up"),
    OTP_FAILED(Party.SYSTEM, "Wrong OTP entered 3 times"),
    NO_CAPTAIN_AVAILABLE(Party.SYSTEM, "No captains available");

    /** A nested enum: who can cancel. */
    public enum Party { CUSTOMER, CAPTAIN, SYSTEM }

    private final Party allowedParty;
    private final String description;

    CancellationReason(Party allowedParty, String description) {
        this.allowedParty = allowedParty;
        this.description = description;
    }

    public Party getAllowedParty() {
        return allowedParty;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
