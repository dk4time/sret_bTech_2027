package com.ridehailing.notification;

/** Every event the app tells people about. */
public enum RideEvent {

    RIDE_BOOKED("Booked", false),
    RIDE_OFFERED("New ride offer", false),
    CAPTAIN_ASSIGNED("Captain assigned", true),
    CAPTAIN_ARRIVED("Captain arrived", false),
    RIDE_STARTED("Ride started", false),
    DESTINATION_CHANGED("Destination changed", false),
    RIDE_COMPLETED("Ride completed", true),
    PAYMENT_SUCCESS("Payment successful", true),
    PAYMENT_FAILED("Payment failed", true),
    RIDE_CANCELLED("Ride cancelled", true);

    private final String title;
    private final boolean important;

    RideEvent(String title, boolean important) {
        this.title = title;
        this.important = important;
    }

    public String getTitle() {
        return title;
    }

    /** Important events are also worth an SMS. */
    public boolean isImportant() {
        return important;
    }
}
