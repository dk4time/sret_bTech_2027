package com.ridehailing.model.user;

import com.ridehailing.exception.CaptainBusyException;
import com.ridehailing.exception.CaptainNotEligibleException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.vehicle.Vehicle;
import com.ridehailing.model.vehicle.VehicleType;

import java.time.LocalDateTime;

/**
 * The person who drives. Captain IS-A User (inheritance) and HAS-A Vehicle (COMPOSITION:
 * the vehicle is registered with the captain, is never swapped, and has no meaning in the
 * app without its captain).
 *
 * ENCAPSULATION: the status, location and dues can only change through intent-revealing
 * methods (verifyKyc, goOnline, acceptRide, reachPickup, finishRide, settleDues...), and
 * each of them checks that the change is legal first.
 */
public class Captain extends User {

    // STATIC members: shared counter for ids and the rules every captain follows.
    private static int nextIdNumber = 2001;
    public static final Money CASH_DUES_LIMIT = Money.of(500);
    public static final double FLAG_RATING_THRESHOLD = 4.0;
    public static final int MIN_RATINGS_BEFORE_FLAG = 5;

    private final Vehicle vehicle;             // composition: final, set once
    private final double maxPreferredTripKm;   // captains decline trips longer than this
    private CaptainStatus status;
    private String currentRideId;
    private String pendingOfferRideId;         // an offer waiting for the captain to tap Accept / Reject
    private LocalDateTime availableFrom;       // a captain is never free before their last drop
    private int offersReceived;
    private int offersAccepted;
    private int cancellationsAfterAccepting;
    private int ridesCompleted;
    private Money duesOwed;                    // platform's share of the cash the captain collected

    // CONSTRUCTOR OVERLOADING with this() chaining: the simple form uses sensible defaults.
    public Captain(String name, String phone, Location home, Vehicle vehicle) {
        this(name, phone, home, vehicle, 60.0, 0, 0, Money.ZERO);
    }

    /** A captain who only wants short trips (e.g. heading home soon). */
    public Captain(String name, String phone, Location home, Vehicle vehicle, double maxPreferredTripKm) {
        this(name, phone, home, vehicle, maxPreferredTripKm, 0, 0, Money.ZERO);
    }

    /** A captain who joined earlier: brings past ratings and unpaid cash dues from previous days. */
    public Captain(String name, String phone, Location home, Vehicle vehicle, double maxPreferredTripKm,
                   int pastRatingCount, int pastRatingTotal, Money openingDues) {
        super("CAP-" + (nextIdNumber++), name, phone, home, pastRatingCount, pastRatingTotal); // super() call
        if (vehicle == null) {
            throw new IllegalArgumentException("A captain must register with a vehicle");
        }
        if (maxPreferredTripKm <= 0) {
            throw new IllegalArgumentException("Preferred trip length must be positive");
        }
        this.vehicle = vehicle;
        this.maxPreferredTripKm = maxPreferredTripKm;
        this.duesOwed = openingDues;
        this.status = CaptainStatus.KYC_PENDING; // every new captain starts here
    }

    @Override
    public String getRole() {
        return "Captain";
    }

    // ------------------------------------------------------------ lifecycle

    public void verifyKyc() {
        if (status != CaptainStatus.KYC_PENDING) {
            throw new IllegalStateException(getName() + " is already verified");
        }
        status = CaptainStatus.OFFLINE;
    }

    public boolean isKycVerified() {
        return status != CaptainStatus.KYC_PENDING;
    }

    /** Goes online at a real place (usually home). Only verified, offline captains can do this. */
    public void goOnline(Location at, LocalDateTime now) throws CaptainNotEligibleException {
        if (status == CaptainStatus.KYC_PENDING) {
            throw new CaptainNotEligibleException(getName() + " cannot go online: KYC verification is still pending");
        }
        if (status != CaptainStatus.OFFLINE) {
            throw new CaptainNotEligibleException(getName() + " is already online");
        }
        moveTo(at);
        status = CaptainStatus.AVAILABLE;
        availableFrom = now;
    }

    public void goOffline() throws CaptainBusyException {
        if (status == CaptainStatus.ON_RIDE) {
            throw new CaptainBusyException(getName() + " cannot go offline during an active ride (" + currentRideId + ")");
        }
        if (pendingOfferRideId != null) {
            throw new CaptainBusyException(getName() + " must first accept or reject the offer for " + pendingOfferRideId);
        }
        if (status == CaptainStatus.AVAILABLE) {
            status = CaptainStatus.OFFLINE;
        }
    }

    /**
     * The captain looks at an offer and decides. Deterministic: a captain declines trips longer
     * than their preferred length. Tracks the acceptance rate.
     */
    public boolean considerOffer(double tripKm) {
        return recordOfferDecision(tripKm <= maxPreferredTripKm);
    }

    /** Captain-app flow: an offer is shown on the captain's phone and waits for a decision. */
    public void receiveOffer(String rideId) {
        if (status != CaptainStatus.AVAILABLE || pendingOfferRideId != null) {
            throw new IllegalStateException(getName() + " cannot receive an offer now");
        }
        pendingOfferRideId = rideId;
    }

    /** The captain taps Accept or Reject on the pending offer. Counts towards the acceptance rate. */
    public boolean respondToOffer(boolean accept) {
        if (pendingOfferRideId == null) {
            throw new IllegalStateException(getName() + " has no pending offer");
        }
        pendingOfferRideId = null;
        return recordOfferDecision(accept);
    }

    /** The customer cancelled while the offer was still on the captain's screen. */
    public void withdrawOffer() {
        pendingOfferRideId = null;
    }

    private boolean recordOfferDecision(boolean accepted) {
        offersReceived++;
        if (accepted) {
            offersAccepted++;
        }
        return accepted;
    }

    public boolean hasPendingOffer() {
        return pendingOfferRideId != null;
    }

    public String getPendingOfferRideId() {
        return pendingOfferRideId;
    }

    public void acceptRide(String rideId, LocalDateTime now) throws CaptainBusyException {
        if (!isAvailableAt(now)) {
            throw new CaptainBusyException(getName() + " is not free to take " + rideId);
        }
        status = CaptainStatus.ON_RIDE;
        currentRideId = rideId;
    }

    /** The only way a captain's location changes during a ride: driving to the pickup... */
    public void reachPickup(Location pickup) {
        requireOnRide();
        moveTo(pickup);
    }

    /** ...and dropping the customer. The captain is free again at the drop point. */
    public void finishRide(Location actualDrop, LocalDateTime droppedAt) {
        requireOnRide();
        moveTo(actualDrop);
        ridesCompleted++;
        becomeFree(droppedAt);
    }

    /** Ride cancelled (by customer, OTP failure, no-show): free again where they are now. */
    public void releaseFromCancelledRide(LocalDateTime now) {
        requireOnRide();
        becomeFree(now);
    }

    /** The captain cancelled after accepting: counted against them, free where they are now. */
    public void cancelAcceptedRide(LocalDateTime now) {
        requireOnRide();
        cancellationsAfterAccepting++;
        becomeFree(now);
    }

    private void becomeFree(LocalDateTime now) {
        status = CaptainStatus.AVAILABLE;
        currentRideId = null;
        availableFrom = now;
    }

    private void requireOnRide() {
        if (status != CaptainStatus.ON_RIDE) {
            throw new IllegalStateException(getName() + " is not on a ride");
        }
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return status.canReceiveOffers() && !now.isBefore(availableFrom);
    }

    // ------------------------------------------------------------ cash dues

    public void addDues(Money amount) {
        duesOwed = duesOwed.plus(amount);
    }

    public boolean isBlockedForCash() {
        return duesOwed.isGreaterThan(CASH_DUES_LIMIT);
    }

    /** Pays everything owed to the platform (UPI transfer from the captain app). Returns the amount paid. */
    public Money settleDues() {
        Money settled = duesOwed;
        duesOwed = Money.ZERO;
        return settled;
    }

    // ------------------------------------------------------------ ratings & stats

    public boolean isFlagged() {
        return getRatingCount() >= MIN_RATINGS_BEFORE_FLAG && getAverageRating() < FLAG_RATING_THRESHOLD;
    }

    public double getAcceptanceRate() {
        if (offersReceived == 0) {
            return 100.0;
        }
        return offersAccepted * 100.0 / offersReceived;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public VehicleType getVehicleType() {
        return vehicle.getType();
    }

    public CaptainStatus getStatus() {
        return status;
    }

    public String getCurrentRideId() {
        return currentRideId;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public int getOffersReceived() {
        return offersReceived;
    }

    public int getOffersAccepted() {
        return offersAccepted;
    }

    public int getCancellationsAfterAccepting() {
        return cancellationsAfterAccepting;
    }

    public int getRidesCompleted() {
        return ridesCompleted;
    }

    public Money getDuesOwed() {
        return duesOwed;
    }

    public double getMaxPreferredTripKm() {
        return maxPreferredTripKm;
    }

    // OVERRIDING toString, reusing User's version with super.toString().
    @Override
    public String toString() {
        return super.toString() + " · " + vehicle.getModel() + " " + vehicle.getRegistrationNumber();
    }
}
