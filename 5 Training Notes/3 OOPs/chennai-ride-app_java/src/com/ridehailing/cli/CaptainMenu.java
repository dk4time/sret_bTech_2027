package com.ridehailing.cli;

import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.CaptainStatus;
import com.ridehailing.service.EarningsService;
import com.ridehailing.service.RideService;
import com.ridehailing.time.SimulatedClock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** The captain's phone. Every action is one existing service call; the services enforce every rule. */
public class CaptainMenu extends Menu {

    private Captain captain;

    public CaptainMenu(ConsoleIO io, CliSession session) {
        super("Captain app", io, session);
    }

    @Override
    protected boolean onEnter() {
        captain = pickCaptain();
        return true;
    }

    @Override
    protected String subtitle() {
        return captain.getName() + " · " + captain.getVehicleType() + " · " + captain.getStatus().getLabel()
                + (captain.hasPendingOffer() ? " · ● offer waiting" : "");
    }

    @Override
    protected String[] options() {
        return new String[] {
                "View profile",
                "Go online / go offline",
                "View incoming offer (accept / reject)",
                "Mark arrived at pickup",
                "Start ride (enter the customer's OTP)",
                "Complete ride",
                "Cancel ride / mark customer no-show",
                "Today's earnings",
                "Settle cash dues",
                "Rate customer for last completed ride",
                "Switch captain"
        };
    }

    @Override
    protected void handle(int choice) throws RideHailingException {
        switch (choice) {
            case 1: viewProfile(); break;
            case 2: toggleOnline(); break;
            case 3: handleOffer(); break;
            case 4: markArrived(); break;
            case 5: startRide(); break;
            case 6: completeRide(); break;
            case 7: cancelOrNoShow(); break;
            case 8: showEarnings(); break;
            case 9: settleDues(); break;
            case 10: rateCustomer(); break;
            default: captain = pickCaptain(); break;
        }
    }

    private RideService rides() {
        return session.getWorld().getRideService();
    }

    // ================================================================== pick

    private Captain pickCaptain() {
        List<Captain> captains = session.getWorld().getCaptainService().getAllCaptains();
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < captains.size(); i++) {
            Captain c = captains.get(i);
            rows.add(new String[] {String.valueOf(i + 1), c.hasPendingOffer() ? "●" : "", c.getName(),
                    c.getVehicleType() + " " + c.getVehicle().getRegistrationNumber(), c.getLocation().getName(),
                    c.getStatus().getLabel()});
        }
        io.printHeader("Choose a captain (● = ride offer waiting)");
        io.printTable(new String[] {"#", "●", "Captain", "Vehicle", "Location", "Status"}, rows);
        return captains.get(io.readInt("Captain", 1, captains.size()) - 1);
    }

    // ================================================================== profile & status

    private void viewProfile() {
        io.printInfo("Captain       : " + captain.getName() + " (" + captain.getId() + ", " + captain.getPhone() + ")");
        io.printInfo("Vehicle       : " + captain.getVehicle());
        io.printInfo("Location      : " + captain.getLocation());
        io.printInfo("Status        : " + captain.getStatus().getLabel()
                + (captain.getCurrentRideId() == null ? "" : " (" + captain.getCurrentRideId() + ")"));
        io.printInfo("KYC           : " + (captain.isKycVerified() ? "verified" : "PENDING - cannot go online"));
        io.printInfo(String.format("Rating        : %.2f★ (%d ratings)%s", captain.getAverageRating(),
                captain.getRatingCount(), captain.isFlagged() ? "  ⚑ FLAGGED (below 4.0)" : ""));
        io.printInfo(String.format("Acceptance    : %.0f%% (%d of %d offers)  · cancellations after accepting: %d",
                captain.getAcceptanceRate(), captain.getOffersAccepted(), captain.getOffersReceived(),
                captain.getCancellationsAfterAccepting()));
        io.printInfo("Cash dues     : " + captain.getDuesOwed() + " (limit " + Captain.CASH_DUES_LIMIT + ")"
                + (captain.isBlockedForCash() ? "  ✘ BLOCKED from cash rides - settle dues" : ""));
        io.printInfo("Rides today   : " + captain.getRidesCompleted());
    }

    private void toggleOnline() throws RideHailingException {
        if (captain.getStatus() == CaptainStatus.OFFLINE || captain.getStatus() == CaptainStatus.KYC_PENDING) {
            session.getWorld().getCaptainService().goOnline(captain, captain.getLocation());
            io.printSuccess(captain.getName() + " is online at " + captain.getLocation() + ".");
        } else {
            session.getWorld().getCaptainService().goOffline(captain);
            io.printSuccess(captain.getName() + " is offline.");
        }
    }

    // ================================================================== offers

    private void handleOffer() throws RideHailingException {
        Ride ride = rides().findPendingOfferFor(captain);
        if (ride == null) {
            io.printInfo("No ride offer waiting for " + captain.getName() + ".");
            return;
        }
        io.printInfo("New ride offer " + ride.getId() + " · " + ride.getVehicleType());
        io.printInfo(String.format("  Pickup : %s (%.1f km from you)", ride.getPickup(),
                captain.getLocation().distanceTo(ride.getPickup())));
        io.printInfo(String.format("  Drop   : %s (trip %.1f km)", ride.getDrop(), ride.getRequest().straightLineKm()));
        io.printInfo("  Fare   : about " + ride.getEstimate().getTotalFare() + " · pay by " + ride.getRequest().getPaymentMethod()
                + " · customer " + ride.getCustomer().getName());
        int answer = io.readChoice("Your answer", new String[] {"Accept", "Reject"});
        Captain holder = rides().respondToOffer(ride, captain, answer == 0);
        if (answer == 0) {
            io.printSuccess("Accepted. Drive to " + ride.getPickup() + " - ETA "
                    + SimulatedClock.format(ride.getExpectedArrivalAt()) + ". Then: Mark arrived at pickup.");
        } else {
            io.printInfo("Rejected. " + captain.getName() + " will not see " + ride.getId() + " again.");
            io.printInfo("The offer moved to Captain " + holder.getName() + " (" + holder.getLocation() + ").");
        }
    }

    // ================================================================== the ride

    private Ride currentRide() {
        Ride ride = captain.getCurrentRideId() == null ? null : rides().findRide(captain.getCurrentRideId());
        if (ride == null) {
            throw new IllegalStateException(captain.getName() + " has no active ride.");
        }
        return ride;
    }

    private void markArrived() throws RideHailingException {
        Ride ride = currentRide();
        if (ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED && ride.getExpectedArrivalAt().isAfter(session.getClock().now())) {
            long minutes = Duration.between(session.getClock().now(), ride.getExpectedArrivalAt()).toMinutes();
            io.printInfo("Driving to the pickup at " + ride.getPickup() + " (" + minutes + " min)...");
            session.advanceClockTo(ride.getExpectedArrivalAt());
        }
        rides().captainArrived(ride);
        io.printSuccess("Arrived at " + ride.getPickup() + " at " + SimulatedClock.format(ride.getArrivedAt())
                + ". Waiting is free for 3 minutes; ask the customer for the OTP.");
    }

    private void startRide() throws RideHailingException {
        Ride ride = currentRide();
        String otp = io.readText("OTP from " + ride.getCustomer().getName() + ":", false);
        rides().startRide(ride, otp);
        io.printSuccess("OTP verified - ride started. Expected at " + ride.getDrop() + " by "
                + SimulatedClock.format(ride.getExpectedDropAt()) + ".");
    }

    private void completeRide() throws RideHailingException {
        Ride ride = currentRide();
        if (ride.getStatus() == RideStatus.IN_PROGRESS && ride.getExpectedDropAt().isAfter(session.getClock().now())) {
            long minutes = Duration.between(session.getClock().now(), ride.getExpectedDropAt()).toMinutes();
            io.printInfo("Driving " + ride.getCustomer().getName() + " to " + ride.getDrop() + " (" + minutes + " min)...");
            session.advanceClockTo(ride.getExpectedDropAt());
        }
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            // Either it was never started (the service explains), or autopilot already finished it.
            if (ride.getReceipt() != null) {
                io.printInfo(ride.getId() + " has already ended.");
                return;
            }
        }
        rides().completeTrip(ride);
        session.printReceipt(ride);
        io.printInfo(captain.getName() + " is now free at " + captain.getLocation() + ".");
    }

    private void cancelOrNoShow() throws RideHailingException {
        Ride ride = currentRide();
        int pick = io.readChoice("What happened?", new String[] {
                "I have to cancel (vehicle problem) - the ride is re-matched, customer not charged",
                "Customer did not show up (allowed after 5 minutes of waiting)"});
        if (pick == 0) {
            Captain next = rides().captainCancelsAfterAccepting(ride);
            io.printSuccess(captain.getName() + " cancelled " + ride.getId() + " (cancellations: "
                    + captain.getCancellationsAfterAccepting() + "). " + captain.getName() + " stays at " + captain.getLocation() + ".");
            if (ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED) {
                io.printInfo("Re-matched: Captain " + next.getName() + " is on the way.");
            } else if (next != null) {
                io.printInfo("Re-matched: the offer is now with Captain " + next.getName() + ".");
            }
        } else {
            Money fee = rides().cancelForNoShow(ride);
            io.printSuccess("No-show recorded. " + ride.getCustomer().getName() + " pays " + fee
                    + " on the next ride; you get it minus commission. You are free at " + captain.getLocation() + ".");
        }
    }

    // ================================================================== money & ratings

    private void showEarnings() {
        EarningsService earnings = session.getWorld().getEarningsService();
        EarningsService.DayEarnings day = earnings.getDayEarnings(captain, session.getClock().today());
        io.printInfo("Earnings for " + day.getDay() + " (trips counted on the day they were booked)");
        io.printTable(new String[] {"Rides", "Gross fare", "Commission 20%", "Net earnings", "Cash collected", "Dues added"},
                singleRow(new String[] {String.valueOf(day.getRides()), day.getGrossFare().toString(),
                        day.getCommission().toString(), day.getNetEarnings().toString(),
                        day.getCashCollected().toString(), day.getDuesAdded().toString()}));
        io.printInfo("Owed to the platform now: " + captain.getDuesOwed()
                + (captain.isBlockedForCash() ? "  ✘ blocked from cash rides" : ""));
    }

    private static List<String[]> singleRow(String[] row) {
        List<String[]> rows = new ArrayList<>();
        rows.add(row);
        return rows;
    }

    private void settleDues() {
        Money settled = session.getWorld().getCaptainService().settleDues(captain);
        if (settled.isZero()) {
            io.printInfo(captain.getName() + " owes nothing.");
        } else {
            io.printSuccess(captain.getName() + " paid " + settled + " to the platform. Cash rides allowed: "
                    + (captain.isBlockedForCash() ? "no" : "yes"));
        }
    }

    private void rateCustomer() throws RideHailingException {
        Ride last = null;
        for (Ride ride : rides().getRidesOf(captain)) {
            if (ride.getStatus() == RideStatus.COMPLETED) {
                last = ride;
            }
        }
        if (last == null) {
            io.printInfo(captain.getName() + " has no completed ride to rate.");
            return;
        }
        io.printInfo("Rate " + last.getCustomer().getName() + " for " + last.getId());
        int stars = io.readInt("Stars", 1, 5);
        session.getWorld().getRatingService().rateCustomer(last, stars);
        io.printSuccess(String.format("%s is now at %.2f★", last.getCustomer().getName(), last.getCustomer().getAverageRating()));
    }
}
