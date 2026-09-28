package com.ridehailing.model.vehicle;

/**
 * ABSTRACT CLASS IN THE MIDDLE of a MULTILEVEL hierarchy: Vehicle -> Cab -> CabEconomy/CabPremium.
 *
 * Cab fills in what ALL cabs share (speeds, AC) and still leaves the rates abstract,
 * so it cannot be instantiated itself.
 */
public abstract class Cab extends Vehicle {

    protected Cab(String registrationNumber, String model, String colour) {
        super(registrationNumber, model, colour);
    }

    /** Luggage bags that fit in the boot. Differs per cab class. */
    public abstract int luggageCapacity();

    public boolean hasAirConditioning() {
        return true;
    }

    // Every cab shares the same traffic profile, so these are implemented once here and inherited.
    @Override
    public double averageSpeedKmph() {
        return 40;
    }

    @Override
    public double peakSpeedKmph() {
        return 32;
    }

    // OVERRIDING toString and reusing the parent's version through super.
    @Override
    public String toString() {
        return super.toString() + " · AC · " + luggageCapacity() + " bags";
    }
}
