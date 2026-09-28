package com.ridehailing.service;

import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.common.ServiceArea;
import com.ridehailing.model.ride.FareEstimate;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.OutstandingFee;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.vehicle.Auto;
import com.ridehailing.model.vehicle.Bike;
import com.ridehailing.model.vehicle.CabEconomy;
import com.ridehailing.model.vehicle.CabPremium;
import com.ridehailing.model.vehicle.Vehicle;
import com.ridehailing.model.vehicle.VehicleType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates surge, estimates and final receipts.
 *
 * POLYMORPHISM: FareService never asks "is this a bike or a cab?". It calls
 * vehicle.calculateBaseFare(...) / vehicle.perKmRate() on a Vehicle reference and the right
 * overridden method runs for whatever object is behind it.
 */
public class FareService {

    // STATIC FINAL constants: the platform's fare rules, shared by everyone.
    public static final int GST_PERCENT = 5;
    public static final int NIGHT_CHARGE_PERCENT = 20;
    public static final int FREE_WAITING_MINUTES = 3;
    public static final Money WAITING_CHARGE_PER_MINUTE = Money.of(1);
    public static final BigDecimal MAX_SURGE = new BigDecimal("2.0");
    public static final String FIRSTRIDE = "FIRSTRIDE";
    public static final Money FIRSTRIDE_DISCOUNT = Money.of(50);
    public static final Money FIRSTRIDE_MIN_FARE = Money.of(100);

    private final ServiceArea serviceArea;

    /**
     * One reference vehicle per type = the published rate card. Every Bike has the same rates,
     * so any Bike object can quote a Bike fare before a captain is chosen.
     * UPCASTING: a Bike, an Auto and two Cabs are all stored as plain Vehicle references.
     */
    private final Map<VehicleType, Vehicle> rateCard = new EnumMap<>(VehicleType.class);

    public FareService(ServiceArea serviceArea) {
        this.serviceArea = serviceArea;
        rateCard.put(VehicleType.BIKE, new Bike("TN-01-RC-0001", "Rate card bike"));
        rateCard.put(VehicleType.AUTO, new Auto("TN-01-RC-0002", "Rate card auto"));
        rateCard.put(VehicleType.CAB_ECONOMY, new CabEconomy("TN-01-RC-0003", "Rate card sedan"));
        rateCard.put(VehicleType.CAB_PREMIUM, new CabPremium("TN-01-RC-0004", "Rate card SUV"));
    }

    public Vehicle rateCardFor(VehicleType type) {
        return rateCard.get(type);
    }

    /** Night charge applies from 11:00 PM up to (not including) 5:00 AM. */
    public static boolean isNightTime(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= 23 || hour < 5;
    }

    /** Minutes a given vehicle needs between two points at a given time of day. */
    public int travelMinutes(Vehicle vehicle, Location from, Location to, LocalDateTime at) {
        boolean peak = serviceArea.isPeakHour(at);
        boolean congested = serviceArea.isCongested(from) || serviceArea.isCongested(to);
        return vehicle.travelMinutes(from.distanceTo(to), peak, congested);
    }

    /**
     * Demand vs supply near the pickup, plus the peak-hour hotspot effect.
     * Capped at 2.0x and rounded to one decimal. Shown to the customer before they confirm.
     */
    public BigDecimal calculateSurge(Location pickup, LocalDateTime at, int openRequestsNearby, int freeCaptainsNearby) {
        double surge = 1.0;
        boolean peak = serviceArea.isPeakHour(at);
        if (peak) {
            surge += 0.1;
            if (serviceArea.isHotspot(pickup)) {
                surge += 0.3;
            }
        }
        int demand = openRequestsNearby + 1; // +1 for this request
        int supply = freeCaptainsNearby;
        if (supply == 0) {
            surge += 0.5;
        } else if (demand > supply) {
            surge += 0.25 * (demand - supply) / supply;
        }
        BigDecimal result = BigDecimal.valueOf(surge).setScale(1, RoundingMode.HALF_UP);
        if (result.compareTo(MAX_SURGE) > 0) {
            result = MAX_SURGE;
        }
        return result;
    }

    public FareEstimate estimate(VehicleType type, Location pickup, Location drop, LocalDateTime at,
                                 BigDecimal surge, int nearestCaptainEtaMinutes) {
        Vehicle vehicle = rateCard.get(type);
        double km = pickup.distanceTo(drop);
        int minutes = travelMinutes(vehicle, pickup, drop, at);
        boolean night = isNightTime(at);

        Money fare = vehicle.calculateBaseFare(km, minutes);   // polymorphic call
        Money surgeCharge = fare.times(surge.subtract(BigDecimal.ONE));
        Money nightCharge = night ? fare.plus(surgeCharge).percent(NIGHT_CHARGE_PERCENT) : Money.ZERO;
        Money beforeGst = fare.plus(surgeCharge).plus(nightCharge);
        Money total = beforeGst.plus(beforeGst.percent(GST_PERCENT));
        boolean available = nearestCaptainEtaMinutes >= 0;
        return new FareEstimate(type, km, minutes, surge, night, total, available, nearestCaptainEtaMinutes);
    }

    public boolean isKnownCoupon(String code) {
        return FIRSTRIDE.equals(code);
    }

    /**
     * The final bill, on ACTUAL distance (pickup -> change point -> drop) and ACTUAL minutes.
     * The captain's drive to the pickup is never billed: the route starts at the pickup.
     */
    public FareReceipt generateReceipt(Ride ride, List<OutstandingFee> previousFees) {
        Vehicle vehicle = ride.getCaptain().getVehicle();      // upcast: the real object may be any subclass
        double km = ride.actualDistanceKm();
        long minutes = ride.actualTripMinutes();

        Money baseFare = vehicle.baseFare();
        Money distanceCharge = vehicle.perKmRate().times(km);
        Money timeCharge = vehicle.perMinuteRate().times(minutes);
        Money raw = baseFare.plus(distanceCharge).plus(timeCharge);
        Money minimumTopUp = Money.ZERO;
        if (raw.isLessThan(vehicle.minimumFare())) {
            minimumTopUp = vehicle.minimumFare().minus(raw);
        }
        Money fareBeforeSurge = raw.plus(minimumTopUp);

        long chargedWaiting = Math.max(0, ride.waitingMinutes() - FREE_WAITING_MINUTES);
        Money waitingCharge = WAITING_CHARGE_PER_MINUTE.times(chargedWaiting);

        BigDecimal surge = ride.getSurgeMultiplier();
        Money surgeCharge = fareBeforeSurge.times(surge.subtract(BigDecimal.ONE));
        Money nightCharge = Money.ZERO;
        if (isNightTime(ride.getStartedAt())) {
            nightCharge = fareBeforeSurge.plus(surgeCharge).percent(NIGHT_CHARGE_PERCENT);
        }
        Money tripFare = fareBeforeSurge.plus(waitingCharge).plus(surgeCharge).plus(nightCharge);

        // Coupon: flat off, only when the trip fare is at least ₹100, and never below the minimum fare.
        String couponCode = ride.getRequest().getCouponCode();
        Money couponDiscount = Money.ZERO;
        if (couponCode != null && !tripFare.isLessThan(FIRSTRIDE_MIN_FARE)) {
            Money room = tripFare.minus(vehicle.minimumFare());
            couponDiscount = FIRSTRIDE_DISCOUNT.min(room);
        }

        String summary = ride.getCustomer().getName() + " · " + vehicle.getType() + " · Captain "
                + ride.getCaptain().getName() + " (" + vehicle.getRegistrationNumber() + ")\n"
                + ride.routeDescription() + "\n"
                + String.format("%.2f km · %d min · %s", km, minutes,
                ride.isEndedEarly() ? "ended early" : (ride.isDestinationChanged() ? "destination changed" : "as booked"));
        String distanceLabel = String.format("%.2f km x %s", km, vehicle.perKmRate());
        String timeLabel = minutes + " min x " + vehicle.perMinuteRate();

        return new FareReceipt(ride.getId(), summary, distanceLabel, timeLabel,
                baseFare, distanceCharge, timeCharge, minimumTopUp,
                waitingCharge, (int) chargedWaiting, surge, surgeCharge, nightCharge,
                previousFees, couponCode, couponDiscount,
                GST_PERCENT, ride.getEstimate().getTotalFare());
    }

    public ServiceArea getServiceArea() {
        return serviceArea;
    }
}
