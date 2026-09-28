package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/** Sedan / hatchback cab: Maruti Dzire, Hyundai Aura. */
public final class CabEconomy extends Cab {

    public CabEconomy(String registrationNumber, String model) {
        super(registrationNumber, model, "White");
    }

    @Override
    public VehicleType getType() {
        return VehicleType.CAB_ECONOMY;
    }

    @Override
    public Money baseFare() {
        return Money.of(50);
    }

    @Override
    public Money perKmRate() {
        return Money.of(14);
    }

    @Override
    public Money perMinuteRate() {
        return Money.of("1.50");
    }

    @Override
    public Money minimumFare() {
        return Money.of(80);
    }

    @Override
    public int seatCapacity() {
        return 4;
    }

    @Override
    public int luggageCapacity() {
        return 2;
    }
}
