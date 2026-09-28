package com.ridehailing.cli;

import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.payment.PaymentStatus;
import com.ridehailing.model.ride.FareEstimate;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.service.FareService;
import com.ridehailing.service.RideService;
import com.ridehailing.time.SimulatedClock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** The customer's phone. Reads input, calls the services, prints the result - nothing more. */
public class CustomerMenu extends Menu {

    private Customer customer;

    public CustomerMenu(ConsoleIO io, CliSession session) {
        super("Customer app", io, session);
    }

    @Override
    protected boolean onEnter() {
        customer = pickCustomer();
        return true;
    }

    @Override
    protected String subtitle() {
        return customer.getName() + " @ " + customer.getLocation();
    }

    @Override
    protected String[] options() {
        return new String[] {
                "View profile",
                "Top up wallet",
                "See fare estimates",
                "Book a ride",
                "Track active ride",
                "Cancel ride",
                "Change destination",
                "End trip early",
                "Pay pending ride",
                "Rate last completed ride",
                "Ride history",
                "Switch customer",
                "Travel on your own (metro / walk) to another place"
        };
    }

    @Override
    protected void handle(int choice) throws RideHailingException {
        switch (choice) {
            case 1: viewProfile(); break;
            case 2: topUp(); break;
            case 3: showEstimates(); break;
            case 4: bookRide(); break;
            case 5: trackRide(); break;
            case 6: cancelRide(); break;
            case 7: changeDestination(); break;
            case 8: endTripEarly(); break;
            case 9: payPendingRide(); break;
            case 10: rateLastRide(); break;
            case 11: showHistory(); break;
            case 12: customer = pickCustomer(); break;
            default: travelOnOwn(); break;
        }
    }

    private RideService rides() {
        return session.getWorld().getRideService();
    }

    // ================================================================== pick

    private Customer pickCustomer() {
        List<Customer> customers = session.getWorld().getCustomerService().getAllCustomers();
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            Ride active = c.getActiveRide();
            Ride unpaid = c.getPaymentPendingRide();
            String rideText = active != null ? active.getId() + " " + active.getStatus()
                    : (unpaid != null ? unpaid.getId() + " PAYMENT_PENDING" : "-");
            rows.add(new String[] {String.valueOf(i + 1), c.getName(), c.getLocation().getName(),
                    c.getWalletBalance().toString(), rideText});
        }
        io.printHeader("Choose a customer");
        io.printTable(new String[] {"#", "Customer", "Location", "Wallet", "Active ride"}, rows);
        return customers.get(io.readInt("Customer", 1, customers.size()) - 1);
    }

    // ================================================================== profile & wallet

    private void viewProfile() {
        Ride unpaid = customer.getPaymentPendingRide();
        Ride active = customer.getActiveRide();
        io.printInfo("Name          : " + customer.getName() + " (" + customer.getId() + ", " + customer.getPhone() + ")");
        io.printInfo("Location      : " + customer.getLocation());
        io.printInfo("Wallet        : " + customer.getWalletBalance() + "   UPI: " + customer.getUpiId());
        io.printInfo(String.format("Rating        : %.2f★ (%d ratings)", customer.getAverageRating(), customer.getRatingCount()));
        io.printInfo("Pending fee   : " + customer.getOutstandingFeeTotal()
                + (customer.getOutstandingFeeTotal().isZero() ? "" : " (added to your next ride)"));
        io.printInfo("Active ride   : " + (active == null ? "none" : active.getId() + " " + active.getStatus()));
        io.printInfo("Payment due   : " + (unpaid == null ? "none" : unpaid.getId() + " " + unpaid.getReceipt().getTotal()
                + " PAYMENT_PENDING"));
        io.printInfo("FIRSTRIDE     : " + (customer.hasUsedCoupon(FareService.FIRSTRIDE) ? "already used" : "available"));
    }

    private void topUp() {
        io.printInfo("Wallet balance: " + customer.getWalletBalance() + " (top-ups up to "
                + Customer.MAX_TOP_UP + " per transaction)");
        Money amount = io.readMoney("Top-up amount");
        session.getWorld().getCustomerService().topUpWallet(customer, amount);
        io.printSuccess("Added " + amount + ". Wallet balance: " + customer.getWalletBalance());
    }

    // ================================================================== estimates & booking

    private List<FareEstimate> printEstimates(Location pickup, Location drop) throws RideHailingException {
        List<FareEstimate> estimates = rides().getFareEstimates(pickup, drop);
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < estimates.size(); i++) {
            FareEstimate e = estimates.get(i);
            String surge = e.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0 ? e.getSurgeMultiplier() + "x" : "-";
            rows.add(new String[] {String.valueOf(i + 1), e.getVehicleType().getDisplayName(),
                    String.valueOf(e.getVehicleType().getSeatCapacity()), e.getTotalFare().toString(), surge,
                    e.hasNightCharge() ? "+20%" : "-", String.format("%.2f km", e.getDistanceKm()),
                    e.getTripMinutes() + " min",
                    e.isCaptainAvailable() ? "✔ " + e.getNearestCaptainEtaMinutes() + " min away" : "✘ none nearby"});
        }
        io.printInfo(pickup + " → " + drop + " at " + SimulatedClock.format(session.getClock().now()));
        io.printTable(new String[] {"#", "Vehicle", "Seats", "Fare", "Surge", "Night", "Distance", "Trip", "Nearest captain"},
                rows);
        return estimates;
    }

    private void showEstimates() throws RideHailingException {
        Location pickup = io.readPlace("Pickup", customer.getLocation());
        Location drop = io.readPlace("Drop", null);
        printEstimates(pickup, drop);
    }

    private void bookRide() throws RideHailingException {
        Location pickup = io.readPlace("Pickup", customer.getLocation());
        Location drop = io.readPlace("Drop", null);
        List<FareEstimate> estimates = printEstimates(pickup, drop);

        VehicleType[] types = VehicleType.values();
        VehicleType type = types[io.readInt("Vehicle (row # above)", 1, types.length) - 1];
        int passengers = io.readInt("Passengers", 1, 6, 1);
        PaymentMethod[] methods = PaymentMethod.values();
        String[] methodNames = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            methodNames[i] = methods[i].toString();
        }
        PaymentMethod method = methods[io.readChoice("Payment method", methodNames)];
        String coupon = io.readText("Coupon code (Enter = none):", true).toUpperCase();

        FareEstimate chosen = estimates.get(type.ordinal());
        io.printInfo("Estimated fare " + chosen.getTotalFare()
                + (chosen.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0 ? " incl. " + chosen.getSurgeMultiplier()
                + "x surge" : "") + (coupon.isEmpty() ? "" : ", coupon " + coupon + " applied on the final fare if eligible")
                + (customer.getOutstandingFeeTotal().isZero() ? "" : ", plus " + customer.getOutstandingFeeTotal()
                + " previous cancellation fee"));
        if (!io.readYesNo("Confirm " + type.getDisplayName() + " for " + passengers + ", pay by " + method + "?")) {
            io.printInfo("Not booked.");
            return;
        }
        Ride ride = rides().bookRide(customer, pickup, drop, type, passengers, method, coupon.isEmpty() ? null : coupon);
        io.printSuccess("Booked " + ride.getId() + " · your OTP is " + ride.revealOtpTo(customer)
                + " (tell it to your captain at pickup)");
        if (ride.getStatus() == RideStatus.REQUESTED) {
            Captain offered = ride.getPendingOfferTo();
            io.printInfo("Offer sent to Captain " + offered.getName() + " (" + offered.getLocation() + ").");
            io.printInfo("→ Main menu › Captain app › " + offered.getName() + " › View incoming offer.");
        } else {
            session.letCaptainPickUp(ride);
            io.printSuccess("Captain " + ride.getCaptain().getName() + " picked you up at "
                    + SimulatedClock.format(ride.getStartedAt()) + ". Expected at " + ride.getDrop() + " by "
                    + SimulatedClock.format(ride.getExpectedDropAt()) + ".");
            io.printInfo("→ Move the clock (Clock controls) to finish the trip, or change destination / end early.");
        }
    }

    // ================================================================== during a ride

    private Ride activeRide() {
        Ride ride = customer.getActiveRide();
        if (ride == null) {
            throw new IllegalStateException(customer.getName() + " has no active ride.");
        }
        return ride;
    }

    private void trackRide() {
        Ride ride = activeRide();
        io.printInfo(ride.getId() + " · " + ride.getStatus() + " (" + ride.getStatus().getLabel() + ")");
        io.printInfo(ride.getPickup() + " → " + ride.getDrop() + " · " + ride.getVehicleType() + " · pay by "
                + ride.getRequest().getPaymentMethod());
        Captain captain = ride.getCaptain();
        if (captain != null) {
            io.printInfo("Captain " + captain.getName() + " · " + captain.getVehicle().getModel() + " "
                    + captain.getVehicle().getRegistrationNumber() + " · now at " + captain.getLocation());
        } else if (ride.getPendingOfferTo() != null) {
            io.printInfo("Waiting for Captain " + ride.getPendingOfferTo().getName() + " to accept the offer.");
        }
        if (ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED) {
            io.printInfo("Captain ETA at pickup: " + SimulatedClock.format(ride.getExpectedArrivalAt()));
        } else if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            io.printInfo("Expected at " + ride.getDrop() + " by " + SimulatedClock.format(ride.getExpectedDropAt()));
        }
        io.printInfo("OTP: " + ride.revealOtpTo(customer));
        io.println(ride.timelineAsText());
    }

    private void cancelRide() throws RideHailingException {
        Ride ride = activeRide();
        Money fee = rides().cancellationFeeIfCancelledNow(ride);
        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            io.printInfo("The trip is in progress - it can only be ended early.");
        } else if (fee.isZero()) {
            io.printInfo("Cancelling now is free.");
        } else {
            io.printError("The 2-minute grace period is over (or the captain has arrived): a " + fee
                    + " cancellation fee will be added to your next ride.");
        }
        if (!io.readYesNo("Cancel " + ride.getId() + "?")) {
            io.printInfo("Ride kept.");
            return;
        }
        Money charged = rides().cancelByCustomer(ride);
        io.printSuccess(ride.getId() + " cancelled. Fee: " + charged
                + (charged.isZero() ? "" : " (pending on your account: " + customer.getOutstandingFeeTotal() + ")"));
    }

    private void changeDestination() throws RideHailingException {
        Ride ride = activeRide();
        Location newDrop = io.readPlace("New drop", null);
        Location changePoint = rides().changeDestination(ride, newDrop);
        io.printSuccess("Heading to " + newDrop + " now (changed near " + changePoint + "). New ETA "
                + SimulatedClock.format(ride.getExpectedDropAt()) + ".");
    }

    private void endTripEarly() throws RideHailingException {
        Ride ride = activeRide();
        FareReceipt receipt = rides().endTripEarly(ride);
        io.printSuccess("Trip ended at " + ride.getDrop() + ". You pay only for what you travelled: " + receipt.getTotal());
        session.printReceipt(ride);
    }

    // ================================================================== after a ride

    private void payPendingRide() throws RideHailingException {
        Ride ride = customer.getPaymentPendingRide();
        if (ride == null) {
            io.printInfo(customer.getName() + " has nothing to pay.");
            return;
        }
        io.printInfo(ride.getId() + " owes " + ride.getReceipt().getTotal() + " (wallet " + customer.getWalletBalance() + ")");
        PaymentMethod method = PaymentMethod.values()[io.readChoice("Pay with",
                new String[] {"Retry UPI (" + customer.getUpiId() + ")", "Cash to the captain", "Rapido Wallet"})];
        PaymentStatus status = rides().retryPayment(ride, method);
        if (status == PaymentStatus.SUCCESS) {
            io.printSuccess(ride.getId() + " is now " + ride.getStatus() + ". You can book again.");
        } else {
            io.printError("Payment failed - " + ride.getId() + " is still " + ride.getStatus() + ".");
        }
    }

    private void rateLastRide() throws RideHailingException {
        Ride last = null;
        for (Ride ride : customer.getRideHistory()) {
            if (ride.getStatus() == RideStatus.COMPLETED) {
                last = ride;
            }
        }
        if (last == null) {
            io.printInfo(customer.getName() + " has no completed ride to rate.");
            return;
        }
        io.printInfo("Rate Captain " + last.getCaptain().getName() + " for " + last.getId() + " ("
                + last.getPickup() + " → " + last.getDrop() + ")");
        int stars = io.readInt("Stars", 1, 5);
        session.getWorld().getRatingService().rateCaptain(last, stars);
        io.printSuccess(String.format("Thanks! %s is now at %.2f★ over %d ratings%s", last.getCaptain().getName(),
                last.getCaptain().getAverageRating(), last.getCaptain().getRatingCount(),
                last.getCaptain().isFlagged() ? " (⚑ flagged for review)" : ""));
    }

    private void showHistory() {
        List<String[]> rows = new ArrayList<>();
        for (Ride ride : customer.getRideHistory()) {
            String amount = ride.getReceipt() != null ? ride.getReceipt().getTotal().toString()
                    : (ride.getCancellationFee().isZero() ? "-" : "fee " + ride.getCancellationFee());
            String note = ride.getStatus() == RideStatus.CANCELLED ? ride.getCancellationReason().toString() : "";
            rows.add(new String[] {SimulatedClock.format(ride.getRequestedAt()), ride.getId(), ride.getStatus().toString(),
                    ride.getVehicleType().getDisplayName(), ride.getPickup() + " → " + ride.getDrop(), amount, note});
        }
        io.printTable(new String[] {"Booked", "Ride", "Status", "Vehicle", "Route", "Amount", "Note"}, rows);
    }

    private void travelOnOwn() {
        Location place = io.readPlace("Where did " + customer.getName() + " go (metro / walk / bus)?", null);
        customer.travelOnOwnTo(place);
        io.printSuccess(customer.getName() + " is now at " + customer.getLocation() + ".");
    }
}
