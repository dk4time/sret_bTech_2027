package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/** Three-wheeler auto rickshaw. */
public final class Auto extends Vehicle {

    public Auto(String registrationNumber, String model) {
        super(registrationNumber, model, "Yellow-Black");
    }

    @Override
    public VehicleType getType() {
        return VehicleType.AUTO;
    }

    @Override
    public Money baseFare() {
        return Money.of(25);
    }

    @Override
    public Money perKmRate() {
        return Money.of(11);
    }

    @Override
    public Money perMinuteRate() {
        return Money.of(1);
    }

    @Override
    public Money minimumFare() {
        return Money.of(35);
    }

    @Override
    public int seatCapacity() {
        return 3;
    }

    @Override
    public double averageSpeedKmph() {
        return 30;
    }

    @Override
    public double peakSpeedKmph() {
        return 22;
    }
}
