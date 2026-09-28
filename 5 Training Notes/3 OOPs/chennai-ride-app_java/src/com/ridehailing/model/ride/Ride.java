package com.ridehailing.model.ride;

import com.ridehailing.exception.InvalidOtpException;
import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.time.SimulatedClock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One ride from request to payment.
 *
 * ENCAPSULATION AT ITS STRONGEST: there is NO setStatus(). The status changes only through
 * intent-revealing methods (assignCaptain, markArrived, start, changeDestination, endEarly,
 * reachDestination, complete, cancel...). Each one checks the current status first and
 * throws InvalidRideStatusException for an illegal move. The OTP is private and is only
 * revealed to the ride's own customer.
 *
 * AGGREGATION: a Ride REFERS to a Customer and a Captain. They exist before the ride and
 * carry on after it; the ride does not own them.
 */
public class Ride {

    private static int nextIdNumber = 5001;              // static id counter
    public static final int MAX_OTP_ATTEMPTS = 3;
    public static final double ARRIVAL_TOLERANCE_METERS = 100;

    private final String id;
    private final RideRequest request;
    private final String otp;
    private final BigDecimal surgeMultiplier;           // locked when the customer confirmed
    private final FareEstimate estimate;

    private RideStatus status;
    private Captain captain;
    private Captain pendingOfferTo;                     // captain-app flow: offer shown, no answer yet
    private Location drop;
    private int wrongOtpAttempts;
    private boolean destinationChanged;
    private boolean endedEarly;

    private final List<Location> travelledRoute = new ArrayList<>();
    private Location legStart;
    private LocalDateTime legStartedAt;

    private LocalDateTime assignedAt;
    private LocalDateTime expectedArrivalAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime expectedDropAt;
    private LocalDateTime droppedAt;

    private final Set<Captain> excludedCaptains = new LinkedHashSet<>();
    private final List<String> offerLog = new ArrayList<>();
    private int rejectedOffers;

    private CancellationReason cancellationReason;
    private Money cancellationFee = Money.ZERO;
    private FareReceipt receipt;
    private PaymentMethod paidWith;
    private boolean captainRatedByCustomer;
    private boolean customerRatedByCaptain;

    private final List<TimelineEntry> timeline = new ArrayList<>();

    public Ride(RideRequest request, String otp, BigDecimal surgeMultiplier, FareEstimate estimate) {
        if (otp == null || !otp.matches("\\d{4}")) {
            throw new IllegalArgumentException("OTP must be 4 digits");
        }
        this.id = "RIDE-" + (nextIdNumber++);
        this.request = request;
        this.otp = otp;
        this.surgeMultiplier = surgeMultiplier;
        this.estimate = estimate;
        this.drop = request.getDrop();
        this.status = RideStatus.REQUESTED;
        record(request.getRequestedAt(), "Requested " + request.getVehicleType() + ", "
                + request.getPickup() + " -> " + request.getDrop());
    }

    // ================================================================ matching

    /** Captain-app flow: the offer now waits on this captain's phone. */
    public void offerTo(Captain offeredTo, LocalDateTime at) throws InvalidRideStatusException {
        requireStatus(RideStatus.REQUESTED, "offer to a captain");
        if (pendingOfferTo != null) {
            throw new InvalidRideStatusException(id + " is already waiting for " + pendingOfferTo.getName() + " to respond");
        }
        if (excludedCaptains.contains(offeredTo)) {
            throw new InvalidRideStatusException(offeredTo.getName() + " already rejected or cancelled " + id);
        }
        pendingOfferTo = offeredTo;
        record(at, "Offered to Captain " + offeredTo.getName() + " - waiting for a response");
    }

    public void recordOffer(Captain offeredTo, boolean accepted, double distanceKm, LocalDateTime at)
            throws InvalidRideStatusException {
        requireStatus(RideStatus.REQUESTED, "offer to a captain");
        if (pendingOfferTo != null && !pendingOfferTo.equals(offeredTo)) {
            throw new InvalidRideStatusException(id + " is waiting for " + pendingOfferTo.getName() + ", not "
                    + offeredTo.getName());
        }
        pendingOfferTo = null;
        String entry = String.format("Offer #%d -> %s (%s, %.1f km away): %s", offerLog.size() + 1,
                offeredTo.getName(), offeredTo.getLocation(), distanceKm, accepted ? "ACCEPTED" : "REJECTED");
        offerLog.add(entry);
        if (!accepted) {
            excludedCaptains.add(offeredTo);  // a captain who rejected THIS ride never sees it again
            rejectedOffers++;
        }
    }

    public void assignCaptain(Captain assigned, LocalDateTime at, LocalDateTime etaAt)
            throws InvalidRideStatusException {
        requireStatus(RideStatus.REQUESTED, "assign a captain");
        if (assigned.getVehicleType() != request.getVehicleType()) {
            throw new InvalidRideStatusException(assigned.getName() + " drives a " + assigned.getVehicleType()
                    + ", this ride needs a " + request.getVehicleType());
        }
        this.captain = assigned;
        this.assignedAt = at;
        this.expectedArrivalAt = etaAt;
        this.status = RideStatus.CAPTAIN_ASSIGNED;
        record(at, "Captain " + assigned.getName() + " assigned, ETA " + SimulatedClock.format(etaAt));
    }

    /** Captain cancelled after accepting: back to searching, and that captain is excluded. */
    public Captain unassignCaptain(LocalDateTime at) throws InvalidRideStatusException {
        if (status != RideStatus.CAPTAIN_ASSIGNED && status != RideStatus.CAPTAIN_ARRIVED) {
            throw new InvalidRideStatusException("Cannot remove the captain of " + id + " while " + status);
        }
        Captain leaving = captain;
        excludedCaptains.add(leaving);
        captain = null;
        assignedAt = null;
        expectedArrivalAt = null;
        arrivedAt = null;
        status = RideStatus.REQUESTED;
        record(at, "Captain " + leaving.getName() + " cancelled - searching again");
        return leaving;
    }

    // ================================================================ pickup

    public void markArrived(LocalDateTime at) throws InvalidRideStatusException {
        requireStatus(RideStatus.CAPTAIN_ASSIGNED, "mark the captain arrived");
        if (at.isBefore(expectedArrivalAt)) {
            throw new InvalidRideStatusException("No teleporting: " + captain.getName() + " cannot reach the pickup before "
                    + SimulatedClock.format(expectedArrivalAt));
        }
        if (!captain.getLocation().isWithinMeters(request.getPickup(), ARRIVAL_TOLERANCE_METERS)) {
            throw new InvalidRideStatusException(captain.getName() + " is at " + captain.getLocation()
                    + ", not within " + (int) ARRIVAL_TOLERANCE_METERS + " m of the pickup");
        }
        arrivedAt = at;
        status = RideStatus.CAPTAIN_ARRIVED;
        record(at, "Captain arrived at " + request.getPickup());
    }

    /** Only the customer who booked can see the OTP (it is shown in their app). */
    public String revealOtpTo(Customer requester) {
        if (!request.getCustomer().equals(requester)) {
            throw new IllegalArgumentException("Only " + request.getCustomer().getName() + " can see this OTP");
        }
        return otp;
    }

    /**
     * The captain types the OTP the customer tells them. Three wrong attempts cancel the ride
     * automatically as OTP_FAILED, with no fee.
     */
    public void start(String enteredOtp, LocalDateTime at, int expectedTripMinutes)
            throws InvalidRideStatusException, InvalidOtpException {
        requireStatus(RideStatus.CAPTAIN_ARRIVED, "start");
        if (!otp.equals(enteredOtp)) {
            wrongOtpAttempts++;
            int left = MAX_OTP_ATTEMPTS - wrongOtpAttempts;
            if (left == 0) {
                cancel(CancellationReason.OTP_FAILED, CancellationReason.Party.SYSTEM, Money.ZERO, at);
                throw new InvalidOtpException("Wrong OTP " + enteredOtp + " - 3 wrong attempts, ride auto-cancelled", 0);
            }
            record(at, "Wrong OTP " + enteredOtp + " (" + left + " attempt(s) left)");
            throw new InvalidOtpException("Wrong OTP " + enteredOtp + ", " + left + " attempt(s) left", left);
        }
        startedAt = at;
        legStart = request.getPickup();
        legStartedAt = at;
        expectedDropAt = at.plusMinutes(expectedTripMinutes);
        travelledRoute.add(request.getPickup());
        status = RideStatus.IN_PROGRESS;
        record(at, "Trip started (OTP verified), expected drop " + SimulatedClock.format(expectedDropAt));
    }

    // ================================================================ in the trip

    /** Where the vehicle is right now on the current leg (straight-line progress by time). */
    public Location positionAt(LocalDateTime at) throws InvalidRideStatusException {
        requireStatus(RideStatus.IN_PROGRESS, "locate the vehicle");
        long legMinutes = Duration.between(legStartedAt, expectedDropAt).toMinutes();
        long elapsed = Duration.between(legStartedAt, at).toMinutes();
        double fraction = legMinutes == 0 ? 1.0 : Math.min(1.0, (double) elapsed / legMinutes);
        Location raw = legStart.pointTowards(drop, fraction, "GPS point");
        return ChennaiPlaces.nameGpsPoint(raw);
    }

    /** Allowed once. Returns the point where the change happened. */
    public Location changeDestination(Location newDrop, LocalDateTime at, int minutesFromHere)
            throws InvalidRideStatusException {
        requireStatus(RideStatus.IN_PROGRESS, "change destination");
        if (droppedAt != null) {
            throw new InvalidRideStatusException("The trip has already ended");
        }
        if (destinationChanged) {
            throw new InvalidRideStatusException("Destination can be changed only once per ride");
        }
        Location changePoint = positionAt(at);
        Location oldDrop = drop;
        travelledRoute.add(changePoint);
        legStart = changePoint;
        legStartedAt = at;
        drop = newDrop;
        expectedDropAt = at.plusMinutes(minutesFromHere);
        destinationChanged = true;
        record(at, "Destination changed " + oldDrop + " -> " + newDrop + " (change point: " + changePoint + ")");
        return changePoint;
    }

    /** Customer ends the ride mid-way. The stop point becomes the actual drop. */
    public Location endEarly(LocalDateTime at) throws InvalidRideStatusException {
        requireStatus(RideStatus.IN_PROGRESS, "end early");
        if (droppedAt != null) {
            throw new InvalidRideStatusException("The trip has already ended");
        }
        Location stopPoint = positionAt(at);
        travelledRoute.add(stopPoint);
        drop = stopPoint;
        droppedAt = at;
        endedEarly = true;
        record(at, "Customer ended the trip early (stop point: " + stopPoint + ")");
        return stopPoint;
    }

    /** Normal arrival at the drop. Cannot happen before the vehicle could physically get there. */
    public void reachDestination(LocalDateTime at) throws InvalidRideStatusException {
        requireStatus(RideStatus.IN_PROGRESS, "reach the destination");
        if (droppedAt != null) {
            throw new InvalidRideStatusException("The trip has already ended");
        }
        if (at.isBefore(expectedDropAt)) {
            throw new InvalidRideStatusException("No teleporting: " + drop + " cannot be reached before "
                    + SimulatedClock.format(expectedDropAt));
        }
        travelledRoute.add(drop);
        droppedAt = at;
        record(at, "Reached " + drop);
    }

    // ================================================================ money

    public void attachReceipt(FareReceipt fareReceipt) throws InvalidRideStatusException {
        requireStatus(RideStatus.IN_PROGRESS, "attach a receipt");
        if (droppedAt == null) {
            throw new InvalidRideStatusException("Cannot bill " + id + " before the customer is dropped");
        }
        if (receipt != null) {
            throw new InvalidRideStatusException(id + " already has a receipt");
        }
        receipt = fareReceipt;
    }

    /** Payment succeeded: the only way into COMPLETED. A ride can be paid once. */
    public void complete(PaymentMethod method, LocalDateTime at) throws InvalidRideStatusException {
        if (status == RideStatus.COMPLETED) {
            throw new InvalidRideStatusException(id + " is already paid - a payment can never succeed twice");
        }
        if (receipt == null || (status != RideStatus.IN_PROGRESS && status != RideStatus.PAYMENT_PENDING)) {
            throw new InvalidRideStatusException("Cannot complete " + id + " while " + status
                    + (receipt == null ? " (trip not billed yet)" : ""));
        }
        paidWith = method;
        status = RideStatus.COMPLETED;
        record(at, "Paid " + receipt.getTotal() + " by " + method + " - ride completed");
    }

    public void markPaymentPending(PaymentMethod triedMethod, String reason, LocalDateTime at)
            throws InvalidRideStatusException {
        if (receipt == null || (status != RideStatus.IN_PROGRESS && status != RideStatus.PAYMENT_PENDING)) {
            throw new InvalidRideStatusException("Cannot mark " + id + " payment pending while " + status);
        }
        status = RideStatus.PAYMENT_PENDING;
        record(at, triedMethod + " payment failed (" + reason + ")");
    }

    // ================================================================ cancel

    public void cancel(CancellationReason reason, CancellationReason.Party by, Money fee, LocalDateTime at)
            throws InvalidRideStatusException {
        if (status == RideStatus.IN_PROGRESS) {
            throw new InvalidRideStatusException(id + " is in progress and cannot be cancelled - end the trip early instead");
        }
        if (!status.canBeCancelled()) {
            throw new InvalidRideStatusException("Cannot cancel " + id + " while " + status);
        }
        if (reason.getAllowedParty() != by) {
            throw new InvalidRideStatusException(by + " cannot cancel with reason '" + reason + "'");
        }
        cancellationReason = reason;
        cancellationFee = fee;
        pendingOfferTo = null;
        status = RideStatus.CANCELLED;
        record(at, "Cancelled by " + by + ": " + reason + (fee.isZero() ? " (no fee)" : " (fee " + fee + ")"));
    }

    // ================================================================ ratings

    public void markCaptainRated() throws InvalidRideStatusException {
        requireStatus(RideStatus.COMPLETED, "rate the captain");
        if (captainRatedByCustomer) {
            throw new InvalidRideStatusException("The customer already rated the captain for " + id);
        }
        captainRatedByCustomer = true;
    }

    public void markCustomerRated() throws InvalidRideStatusException {
        requireStatus(RideStatus.COMPLETED, "rate the customer");
        if (customerRatedByCaptain) {
            throw new InvalidRideStatusException("The captain already rated the customer for " + id);
        }
        customerRatedByCaptain = true;
    }

    // ================================================================ helpers

    private void requireStatus(RideStatus expected, String action) throws InvalidRideStatusException {
        if (status != expected) {
            throw new InvalidRideStatusException("Cannot " + action + " for " + id + ": status is "
                    + status + ", expected " + expected);
        }
    }

    private void record(LocalDateTime at, String note) {
        timeline.add(new TimelineEntry(at, status, note));
    }

    /** Distance actually travelled with the customer: pickup -> (change point) -> actual drop. */
    public double actualDistanceKm() {
        double km = 0;
        for (int i = 1; i < travelledRoute.size(); i++) {
            km += travelledRoute.get(i - 1).distanceTo(travelledRoute.get(i));
        }
        return km;
    }

    public long actualTripMinutes() {
        if (startedAt == null || droppedAt == null) {
            return 0;
        }
        return Duration.between(startedAt, droppedAt).toMinutes();
    }

    public long waitingMinutes() {
        if (arrivedAt == null || startedAt == null) {
            return 0;
        }
        return Duration.between(arrivedAt, startedAt).toMinutes();
    }

    public String routeDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < travelledRoute.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(travelledRoute.get(i).getName());
        }
        return sb.toString();
    }

    public String timelineAsText() {
        StringBuilder sb = new StringBuilder("  Timeline of " + id + " (" + request.getCustomer().getName() + ")\n");
        for (TimelineEntry entry : timeline) {
            sb.append(String.format("    %s  %-17s %s%n", SimulatedClock.format(entry.getAt()),
                    entry.getStatus(), entry.getNote()));
        }
        return sb.toString();
    }

    public boolean isExcluded(Captain c) {
        return excludedCaptains.contains(c);
    }

    // ================================================================ getters (no setters!)

    public String getId() {
        return id;
    }

    public RideRequest getRequest() {
        return request;
    }

    public Customer getCustomer() {
        return request.getCustomer();
    }

    public Captain getCaptain() {
        return captain;
    }

    public Captain getPendingOfferTo() {
        return pendingOfferTo;
    }

    public VehicleType getVehicleType() {
        return request.getVehicleType();
    }

    public Location getPickup() {
        return request.getPickup();
    }

    public Location getDrop() {
        return drop;
    }

    public RideStatus getStatus() {
        return status;
    }

    public BigDecimal getSurgeMultiplier() {
        return surgeMultiplier;
    }

    public FareEstimate getEstimate() {
        return estimate;
    }

    public LocalDateTime getRequestedAt() {
        return request.getRequestedAt();
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getExpectedArrivalAt() {
        return expectedArrivalAt;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getExpectedDropAt() {
        return expectedDropAt;
    }

    public LocalDateTime getDroppedAt() {
        return droppedAt;
    }

    public boolean isDestinationChanged() {
        return destinationChanged;
    }

    public boolean isEndedEarly() {
        return endedEarly;
    }

    public int getWrongOtpAttempts() {
        return wrongOtpAttempts;
    }

    public int getRejectedOfferCount() {
        return rejectedOffers;
    }

    public List<String> getOfferLog() {
        return Collections.unmodifiableList(offerLog);
    }

    public CancellationReason getCancellationReason() {
        return cancellationReason;
    }

    public Money getCancellationFee() {
        return cancellationFee;
    }

    public FareReceipt getReceipt() {
        return receipt;
    }

    public PaymentMethod getPaidWith() {
        return paidWith;
    }

    public List<TimelineEntry> getTimeline() {
        return Collections.unmodifiableList(timeline);
    }

    // ENTITY equality: rides are identified by id.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ride)) {
            return false;
        }
        return id.equals(((Ride) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        String who = captain == null ? "no captain" : captain.getName();
        return id + " [" + status + "] " + request.getCustomer().getName() + ", " + request.getPickup()
                + " -> " + drop + ", " + request.getVehicleType() + ", " + who;
    }
}
