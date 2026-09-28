package com.ridehailing.model.vehicle;

import com.ridehailing.model.common.Money;

/**
 * ENUM WITH FIELDS, A CONSTRUCTOR AND METHODS.
 * Each constant carries its display name, the seats it offers to customers, and the fees
 * charged for a late cancellation or a customer no-show.
 */
public enum VehicleType {

    BIKE("Bike", 1, 20, 25),
    AUTO("Auto", 3, 30, 30),
    CAB_ECONOMY("Cab Economy", 4, 30, 40),
    CAB_PREMIUM("Cab Premium", 6, 30, 50);

    private final String displayName;
    private final int seatCapacity;
    private final Money cancellationFee;
    private final Money noShowFee;

    // Enum constructors are always private.
    VehicleType(String displayName, int seatCapacity, long cancellationFeeRupees, long noShowFeeRupees) {
        this.displayName = displayName;
        this.seatCapacity = seatCapacity;
        this.cancellationFee = Money.of(cancellationFeeRupees);
        this.noShowFee = Money.of(noShowFeeRupees);
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public Money getCancellationFee() {
        return cancellationFee;
    }

    public Money getNoShowFee() {
        return noShowFee;
    }

    public boolean canCarry(int passengers) {
        return passengers >= 1 && passengers <= seatCapacity;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
