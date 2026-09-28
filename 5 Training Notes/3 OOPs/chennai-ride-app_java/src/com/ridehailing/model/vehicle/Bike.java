package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/** Cheapest option. FINAL CLASS: a Bike cannot be extended further. */
public final class Bike extends Vehicle {

    public Bike(String registrationNumber, String model) {
        super(registrationNumber, model); // super(): the Vehicle part is built first
    }

    public Bike(String registrationNumber, String model, String colour) {
        super(registrationNumber, model, colour);
    }

    // OVERRIDING: Bike supplies its own rates.
    @Override
    public VehicleType getType() {
        return VehicleType.BIKE;
    }

    @Override
    public Money baseFare() {
        return Money.of(15);
    }

    @Override
    public Money perKmRate() {
        return Money.of(5);
    }

    @Override
    public Money perMinuteRate() {
        return Money.of("0.50");
    }

    @Override
    public Money minimumFare() {
        return Money.of(25);
    }

    @Override
    public int seatCapacity() {
        return 1;
    }

    @Override
    public double averageSpeedKmph() {
        return 35;
    }

    @Override
    public double peakSpeedKmph() {
        return 28; // bikes weave through traffic better than anyone else
    }
}
