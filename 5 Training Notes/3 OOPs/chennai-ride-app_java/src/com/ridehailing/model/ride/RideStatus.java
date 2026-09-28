package com.ridehailing.model.ride;

/**
 * REQUESTED -> CAPTAIN_ASSIGNED -> CAPTAIN_ARRIVED -> IN_PROGRESS -> COMPLETED
 *                                                               \-> PAYMENT_PENDING -> COMPLETED
 * CANCELLED is reachable from REQUESTED, CAPTAIN_ASSIGNED and CAPTAIN_ARRIVED.
 */
public enum RideStatus {

    REQUESTED("Searching for captain", true),
    CAPTAIN_ASSIGNED("Captain on the way", true),
    CAPTAIN_ARRIVED("Captain at pickup", true),
    IN_PROGRESS("Trip in progress", true),
    PAYMENT_PENDING("Trip over · payment pending", false),
    COMPLETED("Completed", false),
    CANCELLED("Cancelled", false);

    private final String label;
    private final boolean active;

    RideStatus(String label, boolean active) {
        this.label = label;
        this.active = active;
    }

    /** Active = the customer is still waiting for, or sitting in, this ride. */
    public boolean isActive() {
        return active;
    }

    public boolean canBeCancelled() {
        return this == REQUESTED || this == CAPTAIN_ASSIGNED || this == CAPTAIN_ARRIVED;
    }

    public String getLabel() {
        return label;
    }
}
