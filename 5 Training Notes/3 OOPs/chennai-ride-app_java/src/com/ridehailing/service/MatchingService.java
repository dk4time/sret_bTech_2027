package com.ridehailing.service;

import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.exception.NoCaptainAvailableException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.CaptainStatus;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds captains for a ride and offers it to them, nearest first.
 *
 * A captain is eligible only if ALL of these are true: KYC verified, online and free,
 * free since before now, same vehicle type, within the radius, not blocked for cash
 * (on cash rides), and has not already rejected or cancelled THIS ride.
 */
public class MatchingService {

    public static final double MATCH_RADIUS_KM = 7.5;
    public static final int MAX_REJECTED_OFFERS = 3;

    private final CaptainService captainService;
    private final FareService fareService;

    public MatchingService(CaptainService captainService, FareService fareService) {
        this.captainService = captainService;
        this.fareService = fareService;
    }

    /**
     * Returns null when the captain is eligible, otherwise a readable reason (used in the demo, SelfCheck and the
     * operations dashboard). A null type means "any vehicle type".
     */
    public String ineligibilityReason(Captain captain, Location pickup, VehicleType type,
                                      PaymentMethod paymentMethod, Ride ride, LocalDateTime now) {
        if (captain.getStatus() == CaptainStatus.KYC_PENDING) {
            return "KYC pending";
        }
        if (captain.getStatus() == CaptainStatus.OFFLINE) {
            return "offline";
        }
        if (captain.getStatus() == CaptainStatus.ON_RIDE) {
            return "busy on " + captain.getCurrentRideId();
        }
        if (captain.hasPendingOffer()) {
            return "deciding on the offer for " + captain.getPendingOfferRideId();
        }
        if (!captain.isAvailableAt(now)) {
            return "not free until " + SimulatedClock.format(captain.getAvailableFrom());
        }
        if (type != null && captain.getVehicleType() != type) {
            return "wrong vehicle (" + captain.getVehicleType() + ", needs " + type + ")";
        }
        double km = captain.getLocation().distanceTo(pickup);
        if (km > MATCH_RADIUS_KM) {
            return String.format("too far (%.1f km at %s)", km, captain.getLocation());
        }
        if (paymentMethod == PaymentMethod.CASH && captain.isBlockedForCash()) {
            return "blocked for cash rides (owes " + captain.getDuesOwed() + ")";
        }
        if (ride != null && ride.isExcluded(captain)) {
            return "already rejected/cancelled this ride";
        }
        return null;
    }

    /** Eligible captains, nearest first. */
    public List<Captain> findEligibleCaptains(Location pickup, VehicleType type, PaymentMethod paymentMethod,
                                              Ride ride, LocalDateTime now) {
        List<Captain> eligible = new ArrayList<>();
        for (Captain captain : captainService.getAllCaptains()) {
            if (ineligibilityReason(captain, pickup, type, paymentMethod, ride, now) == null) {
                eligible.add(captain);
            }
        }
        // An explicit Comparator: nearest first; the id breaks ties so the order is always the same.
        eligible.sort(new Comparator<Captain>() {
            @Override
            public int compare(Captain a, Captain b) {
                int byDistance = Double.compare(a.getLocation().distanceTo(pickup), b.getLocation().distanceTo(pickup));
                if (byDistance != 0) {
                    return byDistance;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        return eligible;
    }

    /** Free captains of this type within the radius (payment method ignored) - the "supply" for surge. */
    public int countFreeCaptainsNear(Location pickup, VehicleType type, LocalDateTime now) {
        return findEligibleCaptains(pickup, type, PaymentMethod.UPI, null, now).size();
    }

    /** Minutes for this captain to reach the pickup, with their own vehicle's speed and the traffic now. */
    public int etaMinutes(Captain captain, Location pickup, LocalDateTime now) {
        return fareService.travelMinutes(captain.getVehicle(), captain.getLocation(), pickup, now);
    }

    /** ETA of the nearest eligible captain, or -1 if there is nobody. */
    public int nearestEtaMinutes(Location pickup, VehicleType type, LocalDateTime now) {
        List<Captain> eligible = findEligibleCaptains(pickup, type, PaymentMethod.UPI, null, now);
        if (eligible.isEmpty()) {
            return -1;
        }
        return etaMinutes(eligible.get(0), pickup, now);
    }

    /**
     * Offers the ride to eligible captains one by one, nearest first. A rejection moves the
     * offer to the next-nearest captain. After 3 rejections, or when nobody is left, there is
     * no captain for this ride.
     */
    public Captain dispatch(Ride ride, LocalDateTime now) throws NoCaptainAvailableException, InvalidRideStatusException {
        List<Captain> candidates = findEligibleCaptains(ride.getPickup(), ride.getVehicleType(),
                ride.getRequest().getPaymentMethod(), ride, now);
        double tripKm = ride.getRequest().straightLineKm();
        for (Captain captain : candidates) {
            if (ride.getRejectedOfferCount() >= MAX_REJECTED_OFFERS) {
                break;
            }
            boolean accepted = captain.considerOffer(tripKm);
            ride.recordOffer(captain, accepted, captain.getLocation().distanceTo(ride.getPickup()), now);
            if (accepted) {
                return captain;
            }
        }
        throw new NoCaptainAvailableException(noCaptainMessage(ride));
    }

    /**
     * Captain-app flow: the captain the ride should be offered to next (nearest eligible who has not
     * rejected it), or null when nobody is left or 3 captains already declined.
     */
    public Captain nextCandidate(Ride ride, LocalDateTime now) {
        if (ride.getRejectedOfferCount() >= MAX_REJECTED_OFFERS) {
            return null;
        }
        List<Captain> candidates = findEligibleCaptains(ride.getPickup(), ride.getVehicleType(),
                ride.getRequest().getPaymentMethod(), ride, now);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0);
    }

    public String noCaptainMessage(Ride ride) {
        String detail = ride.getRejectedOfferCount() >= MAX_REJECTED_OFFERS
                ? " (" + MAX_REJECTED_OFFERS + " captains declined)"
                : "";
        return "No captains available for " + ride.getVehicleType() + " near " + ride.getPickup() + detail;
    }
}
