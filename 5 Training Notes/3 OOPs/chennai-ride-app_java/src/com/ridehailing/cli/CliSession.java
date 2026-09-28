package com.ridehailing.cli;

import com.ridehailing.app.ChennaiWorld;
import com.ridehailing.exception.CaptainNotEligibleException;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.user.Captain;
import com.ridehailing.notification.NotificationLog;
import com.ridehailing.notification.Notifier;
import com.ridehailing.notification.PushNotifier;
import com.ridehailing.notification.SmsNotifier;
import com.ridehailing.service.DispatchMode;
import com.ridehailing.service.FareService;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One interactive session: a fresh, seeded ChennaiWorld (same people as the demo, clock at
 * 6:00 AM, captains online at home) plus the session settings.
 *
 * The session never decides anything about rides. It only moves the clock (through the
 * RideAutopilot, so rides in Auto captain mode keep progressing) and prints what happened.
 */
public final class CliSession {

    private final ConsoleIO io;
    private final SimulatedClock clock;
    private final NotificationLog notificationLog = new NotificationLog();
    private final ChennaiWorld world;
    private final List<String> startupNotes = new ArrayList<>();

    public CliSession(ConsoleIO io) {
        this.io = io;
        this.clock = new SimulatedClock(ChennaiWorld.at(6, 0));
        List<Notifier> notifiers = new ArrayList<>();
        notifiers.add(new PushNotifier(clock, notificationLog));
        notifiers.add(new SmsNotifier(clock, notificationLog));
        this.world = new ChennaiWorld(clock, notifiers);
        world.getRideService().setDispatchMode(DispatchMode.CAPTAIN_APP);   // Manual captain by default

        for (Captain captain : world.getCaptainService().getAllCaptains()) {
            try {
                world.getCaptainService().goOnline(captain, captain.getLocation());
            } catch (CaptainNotEligibleException e) {
                startupNotes.add(e.getMessage());
            }
        }
    }

    public void printWelcome() {
        io.printHeader("Interactive mode · " + ChennaiWorld.DAY + " (" + ChennaiWorld.DAY.getDayOfWeek() + ")");
        io.printInfo("Fresh Chennai world: " + world.getCustomerService().getAllCustomers().size() + " customers, "
                + world.getCaptainService().getAllCaptains().size() + " captains, clock at "
                + SimulatedClock.format(clock.now()) + ".");
        io.printInfo(world.getCaptainService().countOnline() + " captains are online at their home areas.");
        for (String note : startupNotes) {
            io.printError(note);
        }
        io.printInfo("At any prompt: b = back, q = quit.");
    }

    /** [ 08:15 AM | Peak hour | Active rides: 2 | Online captains: 11 | Mode: Manual captain ] */
    public String statusLine() {
        LocalDateTime now = clock.now();
        String period;
        if (FareService.isNightTime(now)) {
            period = "Night (+20%)";
        } else if (world.getServiceArea().isPeakHour(now)) {
            period = "Peak hour";
        } else {
            period = "Off-peak";
        }
        return "[ " + SimulatedClock.format(now) + " | " + period
                + " | Active rides: " + world.getRideService().getActiveRides().size()
                + " | Online captains: " + world.getCaptainService().countOnline()
                + " | Mode: " + getMode().getLabel() + " ]";
    }

    // ================================================================== clock

    /**
     * Moves the clock forward. Rides in Auto captain mode progress on the way; any trip that
     * ends prints its receipt. Going backward is refused by the SimulatedClock itself.
     */
    public void advanceClockTo(LocalDateTime target) {
        if (target.isBefore(clock.now())) {
            clock.advanceTo(target);   // the clock refuses and explains why
        }
        List<Ride> tripsEnded = world.getAutopilot().advanceTo(target);
        for (Ride ride : tripsEnded) {
            io.printSuccess("Trip " + ride.getId() + " ended for " + ride.getCustomer().getName() + ":");
            printReceipt(ride);
        }
    }

    public void advanceClockByMinutes(long minutes) {
        advanceClockTo(clock.now().plusMinutes(minutes));
    }

    /** Auto captain mode: the accepted captain drives over, the ride starts with the customer's OTP. */
    public void letCaptainPickUp(Ride ride) {
        world.getAutopilot().driveUntilStarted(ride);
        world.getAutopilot().add(ride);      // keep it on autopilot so it ends when the clock reaches the drop time
    }

    // ================================================================== printing

    public void printReceipt(Ride ride) {
        if (ride.getReceipt() == null) {
            io.printInfo("(no receipt: " + ride.getId() + " is " + ride.getStatus() + ")");
            return;
        }
        io.println(ride.getReceipt().toString());
        if (ride.getStatus() == RideStatus.COMPLETED) {
            io.printSuccess("Paid with " + ride.getPaidWith() + " · " + ride.getId() + " COMPLETED");
        } else {
            io.printError(ride.getId() + " is " + ride.getStatus() + " - pay from Customer app → Pay pending ride");
        }
    }

    // ================================================================== settings & access

    public DispatchMode getMode() {
        return world.getRideService().getDispatchMode();
    }

    public void setMode(DispatchMode mode) {
        world.getRideService().setDispatchMode(mode);
    }

    public boolean isManualCaptainMode() {
        return getMode() == DispatchMode.CAPTAIN_APP;
    }

    public ChennaiWorld getWorld() {
        return world;
    }

    public SimulatedClock getClock() {
        return clock;
    }

    public NotificationLog getNotificationLog() {
        return notificationLog;
    }
}
