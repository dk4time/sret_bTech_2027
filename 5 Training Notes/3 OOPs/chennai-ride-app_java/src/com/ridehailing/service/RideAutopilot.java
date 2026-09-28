package com.ridehailing.service;

import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets rides run by themselves as the clock moves forward: the captain arrives at the ETA,
 * the trip starts one minute later with the customer's OTP, and ends (and is paid) at the
 * expected drop time. It holds NO rules of its own - every step is an ordinary RideService call,
 * so every rule is still checked.
 *
 * Used by the scripted demo for rides that are not the focus of a scene, and by the
 * interactive mode in "Auto captain" mode.
 */
public class RideAutopilot {

    private final SimulatedClock clock;
    private final RideService rideService;
    private final List<Ride> rides = new ArrayList<>();

    public RideAutopilot(SimulatedClock clock, RideService rideService) {
        this.clock = clock;
        this.rideService = rideService;
    }

    public void add(Ride ride) {
        if (!rides.contains(ride)) {
            rides.add(ride);
        }
    }

    public void remove(Ride ride) {
        rides.remove(ride);
    }

    /** When is this ride's next step due? null when there is nothing more to do. */
    public LocalDateTime nextStepTime(Ride ride) {
        switch (ride.getStatus()) {
            case CAPTAIN_ASSIGNED:
                return ride.getExpectedArrivalAt();
            case CAPTAIN_ARRIVED:
                return ride.getArrivedAt().plusMinutes(1);
            case IN_PROGRESS:
                return ride.getExpectedDropAt();
            default:
                return null;
        }
    }

    /** The earliest step due among all rides on autopilot, or null. */
    public LocalDateTime nextEventTime() {
        LocalDateTime earliest = null;
        for (Ride ride : rides) {
            LocalDateTime due = nextStepTime(ride);
            if (due != null && (earliest == null || due.isBefore(earliest))) {
                earliest = due;
            }
        }
        return earliest;
    }

    private void performNextStep(Ride ride) {
        try {
            switch (ride.getStatus()) {
                case CAPTAIN_ASSIGNED:
                    rideService.captainArrived(ride);
                    break;
                case CAPTAIN_ARRIVED:
                    rideService.startRide(ride, ride.revealOtpTo(ride.getCustomer()));
                    break;
                case IN_PROGRESS:
                    rideService.completeTrip(ride);
                    break;
                default:
                    break;
            }
        } catch (RideHailingException e) {
            rides.remove(ride);
            throw new IllegalStateException("Autopilot step failed for " + ride.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Moves the clock forward, running every step that falls due on the way, in time order.
     * Returns the rides whose trip ended during this call. Throws if the target is in the past.
     */
    public List<Ride> advanceTo(LocalDateTime target) {
        List<Ride> tripsEnded = new ArrayList<>();
        while (true) {
            Ride nextRide = null;
            LocalDateTime nextTime = null;
            for (Ride ride : rides) {
                LocalDateTime due = nextStepTime(ride);
                if (due != null && !due.isAfter(target) && (nextTime == null || due.isBefore(nextTime))) {
                    nextRide = ride;
                    nextTime = due;
                }
            }
            if (nextRide == null) {
                break;
            }
            clock.advanceToAtLeast(nextTime);
            boolean wasInTrip = nextRide.getStatus() == RideStatus.IN_PROGRESS;
            performNextStep(nextRide);
            if (wasInTrip) {
                tripsEnded.add(nextRide);
            }
        }
        List<Ride> finished = new ArrayList<>();
        for (Ride ride : rides) {
            if (nextStepTime(ride) == null) {
                finished.add(ride);
            }
        }
        rides.removeAll(finished);
        clock.advanceTo(target);   // throws if someone tries to go back in time
        return tripsEnded;
    }

    public void driveToCompletion(Ride ride) {
        add(ride);
        while (nextStepTime(ride) != null) {
            advanceTo(nextStepTime(ride));
        }
    }

    /** Runs the ride until the trip has started, then takes it off autopilot (the caller drives the rest). */
    public void driveUntilStarted(Ride ride) {
        add(ride);
        while (ride.getStatus() != RideStatus.IN_PROGRESS) {
            LocalDateTime due = nextStepTime(ride);
            if (due == null) {
                remove(ride);
                throw new IllegalStateException(ride.getId() + " cannot start: it is " + ride.getStatus());
            }
            advanceTo(due);
        }
        remove(ride);
    }
}
