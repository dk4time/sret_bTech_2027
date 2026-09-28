package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/**
 * ABSTRACTION: every vehicle has fares and speeds, but only a concrete vehicle knows the
 * actual numbers. Vehicle declares WHAT (abstract methods); Bike, Auto and the Cabs say HOW.
 *
 * INHERITANCE:
 *   hierarchical  Vehicle -> Bike, Vehicle -> Auto, Vehicle -> Cab
 *   multilevel    Vehicle -> Cab -> CabEconomy / CabPremium
 */
public abstract class Vehicle {

    private static final String TN_PLATE_FORMAT = "TN-\\d{2}-[A-Z]{1,2}-\\d{4}";

    // ENCAPSULATION: private final fields. A vehicle's plate and model never change.
    private final String registrationNumber;
    private final String model;
    private final String colour;

    // CONSTRUCTOR OVERLOADING + this() chaining: colour defaults to white.
    protected Vehicle(String registrationNumber, String model) {
        this(registrationNumber, model, "White");
    }

    protected Vehicle(String registrationNumber, String model, String colour) {
        if (registrationNumber == null || !registrationNumber.matches(TN_PLATE_FORMAT)) {
            throw new IllegalArgumentException("Not a valid Tamil Nadu registration number: " + registrationNumber);
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Vehicle model is required");
        }
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.colour = colour;
    }

    // ---- ABSTRACT METHODS: each subclass MUST override these (polymorphism by overriding) ----

    public abstract VehicleType getType();

    public abstract Money baseFare();

    public abstract Money perKmRate();

    public abstract Money perMinuteRate();

    public abstract Money minimumFare();

    public abstract int seatCapacity();

    public abstract double averageSpeedKmph();

    public abstract double peakSpeedKmph();

    /**
     * CENTRAL POLYMORPHISM EXAMPLE. This concrete method is written ONCE here, but it calls
     * the overridden rate methods, so a Bike and a CabPremium give different answers from
     * the same line of code. (It is final so no subclass can change the formula itself.)
     */
    public final Money calculateBaseFare(double distanceKm, long minutes) {
        Money fare = baseFare()
                .plus(perKmRate().times(distanceKm))
                .plus(perMinuteRate().times(minutes));
        return fare.max(minimumFare());
    }

    /**
     * Minutes to cover a distance. During peak hours the vehicle slows down, and in a
     * congested zone (T. Nagar, Koyambedu, OMR) it crawls at 60% of the peak speed.
     */
    public int travelMinutes(double distanceKm, boolean peakHour, boolean congestedZone) {
        double speed = peakHour ? peakSpeedKmph() : averageSpeedKmph();
        if (peakHour && congestedZone) {
            speed = speed * 0.6;
        }
        int minutes = (int) Math.ceil(distanceKm / speed * 60);
        return Math.max(1, minutes);
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getModel() {
        return model;
    }

    public String getColour() {
        return colour;
    }

    // ENTITY equality: a vehicle is identified by its number plate.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        return registrationNumber.equals(((Vehicle) o).registrationNumber);
    }

    @Override
    public int hashCode() {
        return registrationNumber.hashCode();
    }

    @Override
    public String toString() {
        return getType() + " · " + model + " (" + registrationNumber + ")";
    }
}
