package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/** SUV cab: Toyota Innova Crysta. Third level of Vehicle -> Cab -> CabPremium. */
public final class CabPremium extends Cab {

    public CabPremium(String registrationNumber, String model) {
        super(registrationNumber, model, "Silver");
    }

    @Override
    public VehicleType getType() {
        return VehicleType.CAB_PREMIUM;
    }

    @Override
    public Money baseFare() {
        return Money.of(80);
    }

    @Override
    public Money perKmRate() {
        return Money.of(20);
    }

    @Override
    public Money perMinuteRate() {
        return Money.of(2);
    }

    @Override
    public Money minimumFare() {
        return Money.of(150);
    }

    @Override
    public int seatCapacity() {
        return 6;
    }

    @Override
    public int luggageCapacity() {
        return 4;
    }

    // Premium SUVs keep a slightly higher cruising speed on open roads (overrides Cab's value).
    @Override
    public double averageSpeedKmph() {
        return 42;
    }
}
