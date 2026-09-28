package com.ridehailing.model.ride;

import com.ridehailing.model.common.Money;
import com.ridehailing.model.vehicle.VehicleType;

import java.math.BigDecimal;

/** One row of the "choose your ride" screen. Immutable. */
public final class FareEstimate {

    private final VehicleType vehicleType;
    private final double distanceKm;
    private final int tripMinutes;
    private final BigDecimal surgeMultiplier;
    private final boolean nightCharge;
    private final Money totalFare;           // what the customer is shown, GST included
    private final boolean captainAvailable;
    private final int nearestCaptainEtaMinutes;  // -1 when nobody is available

    public FareEstimate(VehicleType vehicleType, double distanceKm, int tripMinutes, BigDecimal surgeMultiplier,
                        boolean nightCharge, Money totalFare, boolean captainAvailable, int nearestCaptainEtaMinutes) {
        this.vehicleType = vehicleType;
        this.distanceKm = distanceKm;
        this.tripMinutes = tripMinutes;
        this.surgeMultiplier = surgeMultiplier;
        this.nightCharge = nightCharge;
        this.totalFare = totalFare;
        this.captainAvailable = captainAvailable;
        this.nearestCaptainEtaMinutes = nearestCaptainEtaMinutes;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public int getTripMinutes() {
        return tripMinutes;
    }

    public BigDecimal getSurgeMultiplier() {
        return surgeMultiplier;
    }

    public boolean hasNightCharge() {
        return nightCharge;
    }

    public Money getTotalFare() {
        return totalFare;
    }

    public boolean isCaptainAvailable() {
        return captainAvailable;
    }

    public int getNearestCaptainEtaMinutes() {
        return nearestCaptainEtaMinutes;
    }

    @Override
    public String toString() {
        String eta = captainAvailable ? nearestCaptainEtaMinutes + " min away" : "no captains nearby";
        String surge = surgeMultiplier.compareTo(BigDecimal.ONE) > 0 ? "  surge " + surgeMultiplier + "x" : "";
        String night = nightCharge ? "  +night" : "";
        return String.format("%-12s %10s   %5.2f km · %2d min   %-18s%s%s",
                vehicleType.getDisplayName(), totalFare, distanceKm, tripMinutes, eta, surge, night);
    }
}
