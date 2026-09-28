package com.ridehailing.cli;

import com.ridehailing.app.ChennaiWorld;
import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.ride.TimelineEntry;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.notification.NotificationLog;
import com.ridehailing.service.EarningsService;
import com.ridehailing.service.MatchingService;
import com.ridehailing.time.SimulatedClock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The operations team's view of the whole city. Read-only: it only looks, never changes anything. */
public class OperationsMenu extends Menu {

    public OperationsMenu(ConsoleIO io, CliSession session) {
        super("Operations dashboard", io, session);
    }

    @Override
    protected String[] options() {
        return new String[] {
                "Captain position board",
                "Live rides",
                "Nearby captains for a place (eligible ✔ / ✘ and why)",
                "Surge status at hotspots",
                "Notification log (latest first)",
                "Earnings summary and books reconciliation",
                "Flagged captains / captains blocked from cash rides"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1: positionBoard(); break;
            case 2: liveRides(); break;
            case 3: nearbyCaptains(); break;
            case 4: surgeStatus(); break;
            case 5: notificationLog(); break;
            case 6: earningsSummary(); break;
            default: flaggedAndBlocked(); break;
        }
    }

    private ChennaiWorld world() {
        return session.getWorld();
    }

    private void positionBoard() {
        EarningsService earnings = world().getEarningsService();
        List<String[]> rows = new ArrayList<>();
        for (Captain c : world().getCaptainService().getAllCaptains()) {
            rows.add(new String[] {c.getName(), c.getVehicleType() + " " + c.getVehicle().getRegistrationNumber(),
                    c.getLocation().getName(), c.getStatus().getLabel() + (c.hasPendingOffer() ? " ●" : ""),
                    String.valueOf(c.getRidesCompleted()), earnings.getTotalNetEarnings(c).toString(),
                    c.getDuesOwed().toString()});
        }
        io.printTable(new String[] {"Captain", "Vehicle", "Location", "Status", "Rides", "Earnings", "Cash dues"}, rows);
    }

    private void liveRides() {
        List<String[]> rows = new ArrayList<>();
        for (Ride ride : world().getRideService().getAllRides()) {
            if (ride.getStatus().isActive() || ride.getStatus() == RideStatus.PAYMENT_PENDING) {
                List<TimelineEntry> timeline = ride.getTimeline();
                String since = SimulatedClock.format(timeline.get(timeline.size() - 1).getAt());
                String captain = ride.getCaptain() != null ? ride.getCaptain().getName()
                        : (ride.getPendingOfferTo() != null ? "offer → " + ride.getPendingOfferTo().getName() : "-");
                rows.add(new String[] {ride.getId(), ride.getCustomer().getName(), captain,
                        ride.getPickup() + " → " + ride.getDrop(), ride.getVehicleType().getDisplayName(),
                        ride.getStatus().toString(), since});
            }
        }
        io.printTable(new String[] {"Ride", "Customer", "Captain", "Route", "Vehicle", "Status", "Since"}, rows);
    }

    /** The key teaching screen: why is (or isn't) each captain matchable for this place? */
    private void nearbyCaptains() {
        Location place = io.readPlace("Place", null);
        String[] typeNames = {"Any vehicle", "Bike", "Auto", "Cab Economy", "Cab Premium"};
        int pick = io.readChoice("Vehicle type", typeNames);
        VehicleType type = pick == 0 ? null : VehicleType.values()[pick - 1];
        MatchingService matching = world().getMatchingService();
        LocalDateTime now = session.getClock().now();

        List<Captain> captains = new ArrayList<>(world().getCaptainService().getAllCaptains());
        captains.sort(new Comparator<Captain>() {
            @Override
            public int compare(Captain a, Captain b) {
                return Double.compare(a.getLocation().distanceTo(place), b.getLocation().distanceTo(place));
            }
        });
        List<String[]> rows = new ArrayList<>();
        for (Captain c : captains) {
            String reason = matching.ineligibilityReason(c, place, type, PaymentMethod.UPI, null, now);
            double km = c.getLocation().distanceTo(place);
            String verdict = reason == null ? "✔ eligible" : "✘ " + reason;
            if (reason == null && c.isBlockedForCash()) {
                verdict = verdict + " (UPI/wallet only - blocked for cash)";
            }
            rows.add(new String[] {c.getName(), c.getVehicleType().getDisplayName(), c.getLocation().getName(),
                    String.format("%.1f km", km), verdict});
        }
        io.printInfo("Captains by distance from " + place + " for " + typeNames[pick]
                + String.format(" (match radius %.1f km)", MatchingService.MATCH_RADIUS_KM));
        io.printTable(new String[] {"Captain", "Vehicle", "Now at", "Distance", "Can take a ride here?"}, rows);
    }

    private void surgeStatus() {
        List<Location> places = new ArrayList<>();
        places.add(ChennaiPlaces.CHENNAI_CENTRAL);
        places.add(ChennaiPlaces.KOYAMBEDU);
        places.add(ChennaiPlaces.CHENNAI_AIRPORT);
        if (io.readYesNo("Add another place?")) {
            places.add(io.readPlace("Place", null));
        }
        List<String[]> rows = new ArrayList<>();
        for (Location place : places) {
            String[] row = new String[VehicleType.values().length + 2];
            row[0] = place.getName();
            row[1] = world().getServiceArea().isHotspot(place) ? "yes" : "no";
            int column = 2;
            for (VehicleType type : VehicleType.values()) {
                BigDecimal surge = world().getRideService().currentSurge(place, type);
                row[column++] = surge + "x";
            }
            rows.add(row);
        }
        io.printInfo("Surge at " + SimulatedClock.format(session.getClock().now())
                + (world().getServiceArea().isPeakHour(session.getClock().now()) ? " (peak hour)" : " (off-peak)"));
        io.printTable(new String[] {"Pickup", "Hotspot", "Bike", "Auto", "Cab Economy", "Cab Premium"}, rows);
    }

    private void notificationLog() {
        NotificationLog log = session.getNotificationLog();
        List<String[]> rows = new ArrayList<>();
        for (NotificationLog.Entry entry : log.latestFirst()) {
            rows.add(new String[] {SimulatedClock.format(entry.getAt()), entry.getChannel(), entry.getRecipient(),
                    entry.getEvent().getTitle(), entry.getMessage()});
        }
        io.printInfo(log.size() + " messages sent so far");
        io.printTable(new String[] {"Time", "Channel", "To", "Event", "Message"}, rows);
    }

    private void earningsSummary() {
        EarningsService earnings = world().getEarningsService();
        List<String[]> rows = new ArrayList<>();
        for (EarningsService.DayEarnings day : earnings.getAllDayEarnings()) {
            rows.add(new String[] {day.getCaptain().getName(), String.valueOf(day.getRides()), day.getGrossFare().toString(),
                    day.getCommission().toString(), day.getNetEarnings().toString(), day.getCashCollected().toString(),
                    day.getCaptain().getDuesOwed().toString()});
        }
        io.printTable(new String[] {"Captain", "Rides", "Gross", "Commission", "Net", "Cash collected", "Dues now"}, rows);
        Money rightSide = earnings.getTotalCaptainEarnings().plus(earnings.getTotalCommission()).plus(earnings.getTotalGst());
        io.printInfo("Paid rides              : " + earnings.getPaidRides());
        io.printInfo("Paid by customers       : " + earnings.getTotalPaidByCustomers());
        io.printInfo("  = Captain earnings    : " + earnings.getTotalCaptainEarnings());
        io.printInfo("  + Platform commission : " + earnings.getTotalCommission());
        io.printInfo("  + GST collected       : " + earnings.getTotalGst() + "   (sum " + rightSide + ")");
        io.printInfo("Collected: cash " + earnings.getTotalCash() + " + digital " + earnings.getTotalDigital());
        if (earnings.isBalanced()) {
            io.printSuccess("Books Balanced");
        } else {
            io.printError("BOOKS DO NOT BALANCE");
        }
    }

    private void flaggedAndBlocked() {
        List<String[]> rows = new ArrayList<>();
        for (Captain c : world().getCaptainService().getAllCaptains()) {
            if (c.isFlagged() || c.isBlockedForCash()) {
                String why = "";
                if (c.isFlagged()) {
                    why = String.format("⚑ rating %.2f★ over %d ratings (< %.1f)", c.getAverageRating(), c.getRatingCount(),
                            Captain.FLAG_RATING_THRESHOLD);
                }
                if (c.isBlockedForCash()) {
                    why = (why.isEmpty() ? "" : why + "; ") + "✘ cash blocked, owes " + c.getDuesOwed();
                }
                rows.add(new String[] {c.getName(), c.getVehicleType().getDisplayName(), why});
            }
        }
        io.printTable(new String[] {"Captain", "Vehicle", "Why"}, rows);
    }
}
