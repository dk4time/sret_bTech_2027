package com.ridehailing.service;

import com.ridehailing.exception.ActiveRideExistsException;
import com.ridehailing.exception.CaptainBusyException;
import com.ridehailing.exception.InsufficientWalletBalanceException;
import com.ridehailing.exception.InvalidBookingException;
import com.ridehailing.exception.InvalidOtpException;
import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.exception.NoCaptainAvailableException;
import com.ridehailing.exception.OutOfServiceAreaException;
import com.ridehailing.exception.PaymentPendingException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.common.ServiceArea;
import com.ridehailing.model.payment.Payable;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.payment.PaymentStatus;
import com.ridehailing.model.ride.CancellationReason;
import com.ridehailing.model.ride.FareEstimate;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.OutstandingFee;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideRequest;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.user.User;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.notification.Notifier;
import com.ridehailing.notification.RideEvent;
import com.ridehailing.time.SimulatedClock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The heart of the app: books rides and drives them through their lifecycle.
 *
 * It does not hold rules about a ride's status (Ride guards that itself), fares (FareService),
 * matching (MatchingService) or money (PaymentService). It coordinates them and tells people
 * what happened through every Notifier in its list.
 */
public class RideService {

    public static final int CANCELLATION_GRACE_MINUTES = 2;
    public static final int NO_SHOW_WAIT_MINUTES = 5;
    public static final double MIN_TRIP_KM = 0.5;
    public static final double MAX_TRIP_KM = 60;
    public static final double PICKUP_GPS_TOLERANCE_KM = 1.0;
    private static final long OTP_SEED = 2024L;

    private final SimulatedClock clock;
    private final ServiceArea serviceArea;
    private final MatchingService matchingService;
    private final FareService fareService;
    private final PaymentService paymentService;
    private final List<Notifier> notifiers;              // SMS, push... any Notifier implementation
    private final Random otpGenerator = new Random(OTP_SEED);
    private final List<Ride> rides = new ArrayList<>();
    private DispatchMode dispatchMode = DispatchMode.AUTOMATIC;

    public RideService(SimulatedClock clock, MatchingService matchingService, FareService fareService,
                       PaymentService paymentService, List<Notifier> notifiers) {
        this.clock = clock;
        this.serviceArea = fareService.getServiceArea();
        this.matchingService = matchingService;
        this.fareService = fareService;
        this.paymentService = paymentService;
        this.notifiers = new ArrayList<>(notifiers);
    }

    /** AUTOMATIC (captains decide instantly - the scripted demo) or CAPTAIN_APP (offers wait for the captain). */
    public void setDispatchMode(DispatchMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Dispatch mode is required");
        }
        this.dispatchMode = mode;
    }

    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    // ==================================================================== estimates

    /** The "choose your ride" screen: every vehicle type side by side. */
    public List<FareEstimate> getFareEstimates(Location pickup, Location drop) throws InvalidBookingException {
        validateRoute(pickup, drop);
        LocalDateTime now = clock.now();
        List<FareEstimate> estimates = new ArrayList<>();
        for (VehicleType type : VehicleType.values()) {
            BigDecimal surge = surgeFor(pickup, type, now);
            int eta = matchingService.nearestEtaMinutes(pickup, type, now);
            estimates.add(fareService.estimate(type, pickup, drop, now, surge, eta));
        }
        return estimates;
    }

    /** Surge a customer would see right now for this pickup and vehicle type. */
    public BigDecimal currentSurge(Location pickup, VehicleType type) {
        return surgeFor(pickup, type, clock.now());
    }

    private BigDecimal surgeFor(Location pickup, VehicleType type, LocalDateTime now) {
        int openRequests = countOpenRequestsNear(pickup);
        int freeCaptains = matchingService.countFreeCaptainsNear(pickup, type, now);
        return fareService.calculateSurge(pickup, now, openRequests, freeCaptains);
    }

    /** Requests near this pickup still waiting for, or waiting at, a pickup. */
    public int countOpenRequestsNear(Location pickup) {
        int count = 0;
        for (Ride ride : rides) {
            boolean open = ride.getStatus() == RideStatus.REQUESTED || ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED;
            if (open && ride.getPickup().isWithinKm(pickup, MatchingService.MATCH_RADIUS_KM)) {
                count++;
            }
        }
        return count;
    }

    // ==================================================================== booking

    // METHOD OVERLOADING: three bookRide() methods with different parameter lists.

    /** Without a payment method: the app defaults to Cash, like the real app. */
    public Ride bookRide(Customer customer, Location pickup, Location drop, VehicleType type, int passengers)
            throws InvalidBookingException, NoCaptainAvailableException {
        return bookRide(customer, pickup, drop, type, passengers, PaymentMethod.CASH);
    }

    public Ride bookRide(Customer customer, Location pickup, Location drop, VehicleType type, int passengers,
                         PaymentMethod paymentMethod) throws InvalidBookingException, NoCaptainAvailableException {
        return bookRide(customer, pickup, drop, type, passengers, paymentMethod, null);
    }

    public Ride bookRide(Customer customer, Location pickup, Location drop, VehicleType type, int passengers,
                         PaymentMethod paymentMethod, String couponCode)
            throws InvalidBookingException, NoCaptainAvailableException {
        validateBooking(customer, pickup, drop, type, passengers, couponCode);
        LocalDateTime now = clock.now();

        BigDecimal surge = surgeFor(pickup, type, now);
        int eta = matchingService.nearestEtaMinutes(pickup, type, now);
        FareEstimate estimate = fareService.estimate(type, pickup, drop, now, surge, eta);
        String otp = String.valueOf(1000 + otpGenerator.nextInt(9000));

        RideRequest request = new RideRequest(customer, pickup, drop, type, passengers, paymentMethod, couponCode, now);
        Ride ride = new Ride(request, otp, surge, estimate);
        rides.add(ride);
        customer.recordRide(ride);

        String surgeText = surge.compareTo(BigDecimal.ONE) > 0 ? " (surge " + surge + "x applied)" : "";
        notify(customer, RideEvent.RIDE_BOOKED, type + " " + pickup + " -> " + drop + ", estimated "
                + estimate.getTotalFare() + surgeText + ", pay by " + paymentMethod + ". Finding your captain...");

        if (dispatchMode == DispatchMode.AUTOMATIC) {
            assignNextCaptain(ride);
        } else {
            offerToNextCaptain(ride);
        }
        return ride;
    }

    private void validateRoute(Location pickup, Location drop) throws InvalidBookingException {
        serviceArea.requireInside(pickup, "Pickup");
        serviceArea.requireInside(drop, "Drop");
        double km = pickup.distanceTo(drop);
        if (km < MIN_TRIP_KM) {
            throw new InvalidBookingException(String.format("Pickup and drop are only %.0f m apart - too short for a ride", km * 1000));
        }
        if (km > MAX_TRIP_KM) {
            throw new InvalidBookingException(String.format("Trip of %.1f km is longer than the %.0f km limit", km, MAX_TRIP_KM));
        }
    }

    private void validateBooking(Customer customer, Location pickup, Location drop, VehicleType type,
                                 int passengers, String couponCode) throws InvalidBookingException {
        Ride active = customer.getActiveRide();
        if (active != null) {
            throw new ActiveRideExistsException(customer.getName() + " already has an active ride: " + active.getId()
                    + " (" + active.getStatus() + ")");
        }
        Ride unpaid = customer.getPaymentPendingRide();
        if (unpaid != null) {
            throw new PaymentPendingException(customer.getName() + " must first pay " + unpaid.getReceipt().getTotal()
                    + " for " + unpaid.getId());
        }
        validateRoute(pickup, drop);
        if (!type.canCarry(passengers)) {
            throw new InvalidBookingException(passengers + " passengers cannot ride a " + type + " (max "
                    + type.getSeatCapacity() + ")");
        }
        if (!customer.getLocation().isWithinKm(pickup, PICKUP_GPS_TOLERANCE_KM)) {
            throw new InvalidBookingException("Pickup " + pickup + " is not where " + customer.getName()
                    + " is (" + customer.getLocation() + ")");
        }
        if (couponCode != null) {
            if (!fareService.isKnownCoupon(couponCode)) {
                throw new InvalidBookingException("Unknown coupon " + couponCode);
            }
            if (customer.hasUsedCoupon(couponCode)) {
                throw new InvalidBookingException(couponCode + " was already used by " + customer.getName());
            }
        }
    }

    /** Offers the ride; on success the captain accepts and starts driving to the pickup. */
    private void assignNextCaptain(Ride ride) throws NoCaptainAvailableException {
        LocalDateTime now = clock.now();
        try {
            Captain captain = matchingService.dispatch(ride, now);
            assign(ride, captain, now);
        } catch (NoCaptainAvailableException e) {
            cancelForNoCaptain(ride, now);
            throw e;
        } catch (InvalidRideStatusException bug) {
            // Matching only returns free captains for REQUESTED rides, so this means a bug.
            throw new IllegalStateException(bug);
        }
    }

    /** The captain accepted: they are now on this ride and driving to the pickup. */
    private void assign(Ride ride, Captain captain, LocalDateTime now) {
        try {
            int eta = matchingService.etaMinutes(captain, ride.getPickup(), now);
            captain.acceptRide(ride.getId(), now);
            ride.assignCaptain(captain, now, now.plusMinutes(eta));
            notify(ride.getCustomer(), RideEvent.CAPTAIN_ASSIGNED, "Your captain " + captain.getName() + " ("
                    + captain.getVehicle().getRegistrationNumber() + ", " + captain.getVehicle().getModel() + ") is "
                    + eta + " mins away near " + captain.getLocation() + ". OTP: " + ride.revealOtpTo(ride.getCustomer()));
            notify(captain, RideEvent.CAPTAIN_ASSIGNED, "New ride " + ride.getId() + ": pick up "
                    + ride.getCustomer().getName() + " at " + ride.getPickup() + " -> " + ride.getDrop());
        } catch (InvalidRideStatusException | CaptainBusyException bug) {
            // Only free captains are offered REQUESTED rides, so this means a bug.
            throw new IllegalStateException(bug);
        }
    }

    private void cancelForNoCaptain(Ride ride, LocalDateTime now) {
        try {
            ride.cancel(CancellationReason.NO_CAPTAIN_AVAILABLE, CancellationReason.Party.SYSTEM, Money.ZERO, now);
        } catch (InvalidRideStatusException impossible) {
            throw new IllegalStateException(impossible);
        }
        notify(ride.getCustomer(), RideEvent.RIDE_CANCELLED, "Sorry, no captains available near "
                + ride.getPickup() + " right now. You have not been charged.");
    }

    // ==================================================================== captain-app offers

    /** CAPTAIN_APP mode: shows the ride on the phone of the nearest eligible captain, who must respond. */
    private void offerToNextCaptain(Ride ride) throws NoCaptainAvailableException {
        LocalDateTime now = clock.now();
        Captain next = matchingService.nextCandidate(ride, now);
        if (next == null) {
            String message = matchingService.noCaptainMessage(ride);
            cancelForNoCaptain(ride, now);
            throw new NoCaptainAvailableException(message);
        }
        try {
            ride.offerTo(next, now);
        } catch (InvalidRideStatusException bug) {
            throw new IllegalStateException(bug);
        }
        next.receiveOffer(ride.getId());
        notify(next, RideEvent.RIDE_OFFERED, String.format("New ride offer %s: pick up %s at %s (%.1f km from you) -> %s,"
                        + " about %s. Accept or reject in the Captain app.", ride.getId(), ride.getCustomer().getName(),
                ride.getPickup(), next.getLocation().distanceTo(ride.getPickup()), ride.getDrop(),
                ride.getEstimate().getTotalFare()));
    }

    /**
     * CAPTAIN_APP mode: the offered captain taps Accept or Reject. A rejection moves the offer to the
     * next-nearest eligible captain (never back to this one). Returns the captain now holding the ride:
     * the same captain on accept, the next offered captain on reject.
     */
    public Captain respondToOffer(Ride ride, Captain captain, boolean accept)
            throws InvalidRideStatusException, NoCaptainAvailableException {
        if (ride.getStatus() != RideStatus.REQUESTED || !captain.equals(ride.getPendingOfferTo())) {
            throw new InvalidRideStatusException(captain.getName() + " has no pending offer for " + ride.getId());
        }
        LocalDateTime now = clock.now();
        captain.respondToOffer(accept);
        ride.recordOffer(captain, accept, captain.getLocation().distanceTo(ride.getPickup()), now);
        if (accept) {
            assign(ride, captain, now);
            return captain;
        }
        offerToNextCaptain(ride);
        return ride.getPendingOfferTo();
    }

    /** The ride currently waiting for this captain's answer, or null. */
    public Ride findPendingOfferFor(Captain captain) {
        for (Ride ride : rides) {
            if (captain.equals(ride.getPendingOfferTo())) {
                return ride;
            }
        }
        return null;
    }

    // ==================================================================== pickup

    /** The captain drives to the pickup. Their location changes ONLY here (and at the drop). */
    public void captainArrived(Ride ride) throws InvalidRideStatusException {
        if (ride.getStatus() != RideStatus.CAPTAIN_ASSIGNED) {
            throw new InvalidRideStatusException("Captain cannot arrive for " + ride.getId() + " while " + ride.getStatus());
        }
        if (clock.now().isBefore(ride.getExpectedArrivalAt())) {
            throw new InvalidRideStatusException("No teleporting: " + ride.getCaptain().getName()
                    + " needs until " + SimulatedClock.format(ride.getExpectedArrivalAt()) + " to reach " + ride.getPickup());
        }
        ride.getCaptain().reachPickup(ride.getPickup());
        ride.markArrived(clock.now());
        notify(ride.getCustomer(), RideEvent.CAPTAIN_ARRIVED, "Captain " + ride.getCaptain().getName()
                + " has arrived at " + ride.getPickup() + ". First " + FareService.FREE_WAITING_MINUTES
                + " minutes of waiting are free.");
    }

    public void startRide(Ride ride, String enteredOtp) throws InvalidRideStatusException, InvalidOtpException {
        if (ride.getStatus() != RideStatus.CAPTAIN_ARRIVED) {
            // checked here too because a ride that never had a captain has no vehicle to time the trip with
            throw new InvalidRideStatusException("Cannot start " + ride.getId() + ": status is " + ride.getStatus()
                    + ", expected CAPTAIN_ARRIVED");
        }
        Captain captain = ride.getCaptain();
        LocalDateTime now = clock.now();
        int tripMinutes = fareService.travelMinutes(captain.getVehicle(), ride.getPickup(), ride.getDrop(), now);
        try {
            ride.start(enteredOtp, now, tripMinutes);
        } catch (InvalidOtpException e) {
            if (ride.getStatus() == RideStatus.CANCELLED) {
                captain.releaseFromCancelledRide(now);
                notify(ride.getCustomer(), RideEvent.RIDE_CANCELLED, ride.getId()
                        + " cancelled: wrong OTP entered 3 times. No cancellation fee.");
                notify(captain, RideEvent.RIDE_CANCELLED, ride.getId() + " auto-cancelled (OTP failed). You are free again.");
            }
            throw e;
        }
        notify(ride.getCustomer(), RideEvent.RIDE_STARTED, "Ride started. Expected at " + ride.getDrop()
                + " by " + SimulatedClock.format(ride.getExpectedDropAt()) + ". Have a safe ride!");
    }

    // ==================================================================== during the trip

    /** Allowed once. The fare will follow the actual route: pickup -> change point -> new drop. */
    public Location changeDestination(Ride ride, Location newDrop) throws InvalidRideStatusException, OutOfServiceAreaException {
        serviceArea.requireInside(newDrop, "New drop");
        LocalDateTime now = clock.now();
        Location here = ride.positionAt(now);
        int minutesFromHere = fareService.travelMinutes(ride.getCaptain().getVehicle(), here, newDrop, now);
        Location changePoint = ride.changeDestination(newDrop, now, minutesFromHere);
        notify(ride.getCustomer(), RideEvent.DESTINATION_CHANGED, "Destination changed to " + newDrop
                + " (change point: " + changePoint + "). New ETA " + SimulatedClock.format(ride.getExpectedDropAt())
                + ". Fare will follow the actual route.");
        notify(ride.getCaptain(), RideEvent.DESTINATION_CHANGED, "New drop for " + ride.getId() + ": " + newDrop);
        return changePoint;
    }

    /** Customer stops the ride mid-way: pays only for the distance and time actually travelled. */
    public FareReceipt endTripEarly(Ride ride) throws InvalidRideStatusException {
        ride.endEarly(clock.now());
        return finishTrip(ride, ride.getRequest().getPaymentMethod());
    }

    public FareReceipt completeTrip(Ride ride) throws InvalidRideStatusException {
        return completeTrip(ride, ride.getRequest().getPaymentMethod());
    }

    /** OVERLOAD: the customer may switch the payment method at the end of the ride. */
    public FareReceipt completeTrip(Ride ride, PaymentMethod payWith) throws InvalidRideStatusException {
        ride.reachDestination(clock.now());
        return finishTrip(ride, payWith);
    }

    private FareReceipt finishTrip(Ride ride, PaymentMethod payWith) throws InvalidRideStatusException {
        LocalDateTime now = clock.now();
        Customer customer = ride.getCustomer();
        Captain captain = ride.getCaptain();
        Location actualDrop = ride.getDrop();

        // Both people are now where the trip really ended.
        captain.finishRide(actualDrop, now);
        customer.arriveAt(actualDrop);

        // Unpaid fees are handed over exactly once and printed on this receipt.
        List<OutstandingFee> fees = customer.takeOutstandingFees();
        FareReceipt receipt = fareService.generateReceipt(ride, fees);
        ride.attachReceipt(receipt);
        if (!receipt.getCouponDiscount().isZero()) {
            customer.markCouponUsed(ride.getRequest().getCouponCode());
        }
        String where = ride.isEndedEarly() ? "Trip ended early (stop point: " + actualDrop + ")" : "You have reached " + actualDrop;
        notify(customer, RideEvent.RIDE_COMPLETED, where + ". Fare "
                + receipt.getTotal() + " (" + String.format("%.2f km, %d min", ride.actualDistanceKm(), ride.actualTripMinutes()) + ")");

        try {
            attemptPayment(ride, payWith);
        } catch (InsufficientWalletBalanceException e) {
            // Already notified inside attemptPayment; the trip itself is over, the ride waits for payment.
        }
        return receipt;
    }

    // ==================================================================== payment

    /** For a PAYMENT_PENDING ride: retry UPI, or switch to cash / wallet. */
    public PaymentStatus retryPayment(Ride ride, PaymentMethod method)
            throws InvalidRideStatusException, InsufficientWalletBalanceException {
        if (ride.getStatus() != RideStatus.PAYMENT_PENDING) {
            throw new InvalidRideStatusException("Nothing to retry for " + ride.getId() + ": status is " + ride.getStatus());
        }
        return attemptPayment(ride, method);
    }

    private PaymentStatus attemptPayment(Ride ride, PaymentMethod method)
            throws InvalidRideStatusException, InsufficientWalletBalanceException {
        Customer customer = ride.getCustomer();
        Payable payment;
        try {
            payment = paymentService.collect(ride, method, clock.now());
        } catch (InsufficientWalletBalanceException e) {
            notify(customer, RideEvent.PAYMENT_FAILED, "Wallet payment failed: balance " + e.getBalance()
                    + " is less than " + e.getRequired() + ". Ride " + ride.getId() + " is PAYMENT_PENDING.");
            throw e;
        }
        if (payment.status() == PaymentStatus.SUCCESS) {
            notify(customer, RideEvent.PAYMENT_SUCCESS, payment.resultMessage());
            notify(ride.getCaptain(), RideEvent.PAYMENT_SUCCESS, ride.getId() + " paid by " + method + ".");
        } else {
            notify(customer, RideEvent.PAYMENT_FAILED, payment.resultMessage() + ". Ride " + ride.getId()
                    + " is PAYMENT_PENDING - retry UPI or pay cash.");
        }
        return payment.status();
    }

    // ==================================================================== cancellations

    /**
     * Customer cancels. Free before a captain is assigned or within 2 minutes of assignment.
     * After that, or once the captain has arrived, a fee is added to the NEXT ride.
     */
    public Money cancelByCustomer(Ride ride) throws InvalidRideStatusException {
        LocalDateTime now = clock.now();
        Money fee = cancellationFeeIfCancelledNow(ride);
        Captain captain = ride.getCaptain();
        Captain offeredTo = ride.getPendingOfferTo();
        ride.cancel(CancellationReason.CUSTOMER_CHANGED_PLANS, CancellationReason.Party.CUSTOMER, fee, now);
        if (offeredTo != null) {
            offeredTo.withdrawOffer();
            notify(offeredTo, RideEvent.RIDE_CANCELLED, ride.getCustomer().getName() + " cancelled " + ride.getId()
                    + " before you responded.");
        }
        if (captain != null) {
            captain.releaseFromCancelledRide(now);
            notify(captain, RideEvent.RIDE_CANCELLED, ride.getCustomer().getName() + " cancelled " + ride.getId()
                    + (fee.isZero() ? "." : ". You will receive the " + fee + " cancellation fee (minus commission)."));
        }
        if (!fee.isZero()) {
            ride.getCustomer().addOutstandingFee(new OutstandingFee(fee, captain, ride.getId(), "late cancellation"));
        }
        notify(ride.getCustomer(), RideEvent.RIDE_CANCELLED, ride.getId() + " cancelled. "
                + (fee.isZero() ? "No cancellation fee." : "Cancellation fee " + fee + " will be added to your next ride."));
        return fee;
    }

    /**
     * What cancelling this ride right now would cost the customer (lets the app warn before confirming).
     * Free while searching or within the grace period; the vehicle's fee after it or once the captain arrived.
     */
    public Money cancellationFeeIfCancelledNow(Ride ride) {
        if (ride.getStatus() == RideStatus.CAPTAIN_ARRIVED) {
            return ride.getVehicleType().getCancellationFee();
        }
        if (ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED) {
            long secondsSinceAssigned = Duration.between(ride.getAssignedAt(), clock.now()).getSeconds();
            if (secondsSinceAssigned > CANCELLATION_GRACE_MINUTES * 60L) {
                return ride.getVehicleType().getCancellationFee();
            }
        }
        return Money.ZERO;
    }

    /** Captain waited 5+ minutes at the pickup and the customer never came. */
    public Money cancelForNoShow(Ride ride) throws InvalidRideStatusException {
        LocalDateTime now = clock.now();
        if (ride.getStatus() != RideStatus.CAPTAIN_ARRIVED) {
            throw new InvalidRideStatusException("No-show needs the captain to be at the pickup; status is " + ride.getStatus());
        }
        long waited = Duration.between(ride.getArrivedAt(), now).toMinutes();
        if (waited < NO_SHOW_WAIT_MINUTES) {
            throw new InvalidRideStatusException("Captain must wait " + NO_SHOW_WAIT_MINUTES
                    + " minutes before a no-show (waited " + waited + ")");
        }
        Money fee = ride.getVehicleType().getNoShowFee();
        Captain captain = ride.getCaptain();
        ride.cancel(CancellationReason.CUSTOMER_NO_SHOW, CancellationReason.Party.CAPTAIN, fee, now);
        captain.releaseFromCancelledRide(now);   // free again, right here at the pickup
        ride.getCustomer().addOutstandingFee(new OutstandingFee(fee, captain, ride.getId(), "no-show"));
        notify(ride.getCustomer(), RideEvent.RIDE_CANCELLED, "Captain " + captain.getName() + " waited " + waited
                + " min at " + ride.getPickup() + ". Ride cancelled as no-show; fee " + fee + " added to your next ride.");
        notify(captain, RideEvent.RIDE_CANCELLED, "No-show recorded for " + ride.getId() + ". You will receive "
                + fee + " (minus commission).");
        return fee;
    }

    /**
     * Captain cancels after accepting: the ride goes back to matching (that captain excluded)
     * and the customer is not charged. Returns the new captain (in CAPTAIN_APP mode: the captain
     * the ride is now offered to).
     */
    public Captain captainCancelsAfterAccepting(Ride ride) throws InvalidRideStatusException, NoCaptainAvailableException {
        LocalDateTime now = clock.now();
        Captain leaving = ride.unassignCaptain(now);
        leaving.cancelAcceptedRide(now);
        notify(ride.getCustomer(), RideEvent.RIDE_CANCELLED, "Captain " + leaving.getName()
                + " had to cancel. Finding you another captain - you will not be charged.");
        if (dispatchMode == DispatchMode.AUTOMATIC) {
            assignNextCaptain(ride);
            return ride.getCaptain();
        }
        offerToNextCaptain(ride);
        return ride.getPendingOfferTo();
    }

    // ==================================================================== queries

    public List<Ride> getAllRides() {
        return Collections.unmodifiableList(rides);
    }

    /** Rides still waiting for, or carrying, their customer. */
    public List<Ride> getActiveRides() {
        List<Ride> active = new ArrayList<>();
        for (Ride ride : rides) {
            if (ride.getStatus().isActive()) {
                active.add(ride);
            }
        }
        return active;
    }

    public Ride findRide(String rideId) {
        for (Ride ride : rides) {
            if (ride.getId().equals(rideId)) {
                return ride;
            }
        }
        return null;
    }

    public List<Ride> getRidesOf(Captain captain) {
        List<Ride> result = new ArrayList<>();
        for (Ride ride : rides) {
            if (captain.equals(ride.getCaptain())) {
                result.add(ride);
            }
        }
        return result;
    }

    private void notify(User recipient, RideEvent event, String message) {
        for (Notifier notifier : notifiers) {
            if (notifier.handles(event)) {
                notifier.notify(recipient, event, message);  // DYNAMIC DISPATCH: SMS or push
            }
        }
    }
}
