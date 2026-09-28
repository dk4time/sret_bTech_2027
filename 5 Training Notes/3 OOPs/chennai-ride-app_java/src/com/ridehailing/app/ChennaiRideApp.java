package com.ridehailing.app;

import com.ridehailing.exception.CaptainBusyException;
import com.ridehailing.exception.CaptainNotEligibleException;
import com.ridehailing.exception.InvalidBookingException;
import com.ridehailing.exception.InvalidOtpException;
import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.exception.NoCaptainAvailableException;
import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.common.ServiceArea;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.payment.PaymentStatus;
import com.ridehailing.model.ride.FareEstimate;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.Vehicle;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.notification.Notifier;
import com.ridehailing.notification.PushNotifier;
import com.ridehailing.notification.SmsNotifier;
import com.ridehailing.service.CaptainService;
import com.ridehailing.service.CustomerService;
import com.ridehailing.service.EarningsService;
import com.ridehailing.service.FareService;
import com.ridehailing.service.MatchingService;
import com.ridehailing.service.RatingService;
import com.ridehailing.service.RideAutopilot;
import com.ridehailing.service.RideService;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One full simulated day of the app in Chennai, told as a story.
 *
 * Many rides overlap in time, exactly like a real day. Rides that are not the focus of a
 * scene are put on "autopilot": as the clock moves forward, their captain arrives, the trip
 * starts, ends and is paid at the right minute. Every line printed carries the clock's time.
 */
public class ChennaiRideApp {

    public static final LocalDate DEMO_DAY = ChennaiWorld.DAY;

    // ---- the app: one seeded ChennaiWorld (composition), and shortcuts to its services ----
    private final SimulatedClock clock = new SimulatedClock(at(5, 55));
    private final ChennaiWorld world;
    private final ServiceArea chennai;
    private final CustomerService customerService;
    private final CaptainService captainService;
    private final MatchingService matchingService;
    private final EarningsService earningsService;
    private final RatingService ratingService;
    private final RideService rideService;
    private final RideAutopilot autopilot;

    // ---- people ----
    private Customer priya;
    private Customer divya;
    private Customer arun;
    private Customer lakshmi;
    private Customer harish;

    private Captain murugan;
    private Captain karthik;
    private Captain selvi;
    private Captain ganesh;
    private Captain suresh;
    private Captain anbu;
    private Captain ramesh;
    private Captain senthil;
    private Captain rajesh;
    private Captain balaji;
    private Captain vignesh;
    private Captain mani;

    // ---- rides we come back to at the end of the day (ratings) ----
    private Ride priyaMorningRide;
    private Ride divyaCentralRide;
    private Ride arunTambaramRide;
    private Ride divyaEgmoreRide;
    private Ride harishChromepetRide;
    private Ride lakshmiVadapalaniRide;
    private Ride divyaWaitingRide;
    private Ride lakshmiOtpRide;
    private Ride priyaOtpFailedRide;
    private Ride arunRematchedRide;
    private Ride lakshmiChangedRide;
    private Ride arunEarlyRide;
    private Ride harishPendingRide;
    private Ride divyaCashRide;
    private Ride divyaSkippedMuruganRide;
    private Ride priyaAirportRide;
    private Ride divyaNightRide;

    public ChennaiRideApp() {
        // UPCASTING into the interface type: the service only knows "a list of Notifiers".
        List<Notifier> notifiers = new ArrayList<>();
        notifiers.add(new PushNotifier(clock));
        notifiers.add(new SmsNotifier(clock));
        world = new ChennaiWorld(clock, notifiers);
        chennai = world.getServiceArea();
        customerService = world.getCustomerService();
        captainService = world.getCaptainService();
        matchingService = world.getMatchingService();
        earningsService = world.getEarningsService();
        ratingService = world.getRatingService();
        rideService = world.getRideService();
        autopilot = world.getAutopilot();
    }

    public static void main(String[] args) throws RideHailingException {
        new ChennaiRideApp().runDay();
    }

    /** Runs every scene in chronological order. */
    public void runDay() throws RideHailingException {
        banner();
        seedPeopleAndVehicles();
        scene01CaptainsComeOnline();
        scene02PriyaComparesAndRidesBike();
        scene03AutoFromCentralWithSurgeAndCash();
        scene04CanonicalEgmoreToTambaram();
        scene05RejectThenAccept();
        scene06SecondBookingWhileActive();
        scene07Cancellations();
        scene08WaitingCharge();
        scene09NoShow();
        scene10OtpAttempts();
        scene11CaptainCancelsAndRematch();
        scene12ChangeDestination();
        scene13EndTripEarly();
        scene14PaymentPending();
        scene15CashDuesBlock();
        scene16NoPremiumAtSholinganallur();
        scene17AirportPremiumNightCoupon();
        scene18OutsideServiceArea();
        scene19OfflineMidRide();
        scene20Ratings();
        scene21EndOfDay();
    }

    // ====================================================================== setup

    /** The people come from the seeded ChennaiWorld; the demo keeps a named reference to each. */
    private void seedPeopleAndVehicles() {
        priya = world.customer("Priya");
        divya = world.customer("Divya");
        arun = world.customer("Arun");
        lakshmi = world.customer("Lakshmi");
        harish = world.customer("Harish");

        murugan = world.captain("Murugan");
        karthik = world.captain("Karthik");
        selvi = world.captain("Selvi");
        ganesh = world.captain("Ganesh");
        suresh = world.captain("Suresh");
        anbu = world.captain("Anbu");
        ramesh = world.captain("Ramesh");
        senthil = world.captain("Senthil");
        rajesh = world.captain("Rajesh");
        balaji = world.captain("Balaji");
        vignesh = world.captain("Vignesh");
        mani = world.captain("Mani");
        say("Registered " + customerService.getAllCustomers().size() + " customers and "
                + captainService.getAllCaptains().size() + " captains (Mani's KYC is still pending).");
    }

    // ====================================================================== scenes

    private void scene01CaptainsComeOnline() {
        scene(1, 6, 0, "Captains come online at their home areas");
        for (Captain captain : captainService.getAllCaptains()) {
            try {
                captainService.goOnline(captain, captain.getLocation());
                say("  " + captain.getName() + " is online at " + captain.getLocation() + " (" + captain.getVehicle() + ")");
            } catch (CaptainNotEligibleException e) {
                say("  ✘ " + e.getMessage());
            }
        }
        printBoard("Captain Position Board - 6:00 AM");
    }

    private void scene02PriyaComparesAndRidesBike() throws RideHailingException {
        scene(2, 8, 15, "Priya compares fares, Velachery -> Guindy (peak hour), books a bike, pays by UPI");
        List<FareEstimate> estimates = rideService.getFareEstimates(priya.getLocation(), ChennaiPlaces.GUINDY);
        say("Priya's screen: choose your ride");
        for (FareEstimate estimate : estimates) {
            say("    " + estimate);
        }
        priyaMorningRide = rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.GUINDY,
                VehicleType.BIKE, 1, PaymentMethod.UPI);
        driveToCompletion(priyaMorningRide);
        printReceipt(priyaMorningRide);
    }

    private void scene03AutoFromCentralWithSurgeAndCash() throws RideHailingException {
        scene(3, 8, 40, "Divya gets off the train at Chennai Central and takes an auto to Egmore (peak surge, cash)");
        Money duesBefore = murugan.getDuesOwed();
        for (FareEstimate estimate : rideService.getFareEstimates(divya.getLocation(), ChennaiPlaces.EGMORE)) {
            if (estimate.getVehicleType() == VehicleType.AUTO) {
                say("Divya's screen before confirming: " + estimate);
            }
        }
        divyaCentralRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.EGMORE,
                VehicleType.AUTO, 2);     // OVERLOADED bookRide without a payment method -> Cash
        say("Surge locked at booking: " + divyaCentralRide.getSurgeMultiplier() + "x (Chennai Central hotspot, peak hour)");
        driveToCompletion(divyaCentralRide);
        printReceipt(divyaCentralRide);
        say("Murugan collected cash. Dues owed to the platform: " + duesBefore + " -> " + murugan.getDuesOwed());
    }

    private void scene04CanonicalEgmoreToTambaram() throws RideHailingException {
        scene(4, 9, 10, "CANONICAL FLOW: a cab from Egmore to Tambaram");
        say("BEFORE: Senthil is at " + senthil.getLocation() + ", Rajesh is at " + rajesh.getLocation());
        arunTambaramRide = rideService.bookRide(arun, arun.getLocation(), ChennaiPlaces.TAMBARAM,
                VehicleType.CAB_ECONOMY, 1, PaymentMethod.WALLET);
        driveToCompletion(arunTambaramRide);
        printReceipt(arunTambaramRide);
        say("AFTER:  Senthil is at " + senthil.getLocation() + " (the actual drop point) and "
                + senthil.getStatus().getLabel());
        System.out.print(arunTambaramRide.timelineAsText());

        say("Next, Divya (still at Egmore) requests a cab from Egmore to Anna Nagar...");
        say("  Senthil for this Egmore request? " + reasonOrEligible(senthil, divya.getLocation(),
                VehicleType.CAB_ECONOMY, PaymentMethod.UPI));
        divyaEgmoreRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.ANNA_NAGAR,
                VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
        say("  -> went to " + divyaEgmoreRide.getCaptain().getName() + " at Royapettah, NOT to Senthil in Tambaram.");
        autopilot(divyaEgmoreRide);

        say("Then Harish requests a cab from Chromepet to Medavakkam...");
        harishChromepetRide = rideService.bookRide(harish, harish.getLocation(), ChennaiPlaces.MEDAVAKKAM,
                VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
        say(String.format("  -> went to %s, who is now at Tambaram (%.1f km from Chromepet).",
                harishChromepetRide.getCaptain().getName(), ChennaiPlaces.TAMBARAM.distanceTo(ChennaiPlaces.CHROMEPET)));
        autopilot(harishChromepetRide);
        printBoard("Captain Position Board - after the canonical ride");
    }

    private void scene05RejectThenAccept() throws RideHailingException {
        scene(5, 10, 0, "Nearest captain rejects, next-nearest accepts");
        say("Lakshmi books a bike, Mylapore -> Vadapalani (" + km(ChennaiPlaces.MYLAPORE, ChennaiPlaces.VADAPALANI) + ")");
        say("Ganesh (Adyar) only takes trips up to " + (int) ganesh.getMaxPreferredTripKm() + " km today - he is heading home soon.");
        lakshmiVadapalaniRide = rideService.bookRide(lakshmi, lakshmi.getLocation(), ChennaiPlaces.VADAPALANI,
                VehicleType.BIKE, 1, PaymentMethod.UPI);
        say("Offer sequence:");
        for (String offer : lakshmiVadapalaniRide.getOfferLog()) {
            say("    " + offer);
        }
        autopilot(lakshmiVadapalaniRide);
    }

    private void scene06SecondBookingWhileActive() throws RideHailingException {
        scene(6, 10, 30, "A customer with an active ride tries to book again");
        say("Lakshmi's ride " + lakshmiVadapalaniRide.getId() + " is " + lakshmiVadapalaniRide.getStatus()
                + ". She taps 'Book' again for an auto...");
        try {
            rideService.bookRide(lakshmi, lakshmi.getLocation(), ChennaiPlaces.T_NAGAR, VehicleType.AUTO, 1);
            say("  (unexpected) second booking went through");
        } catch (InvalidBookingException e) {
            say("  ✘ Rejected (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        driveToCompletion(lakshmiVadapalaniRide);
        say("Lakshmi reached " + lakshmi.getLocation() + ". Fare " + lakshmiVadapalaniRide.getReceipt().getTotal());
    }

    private void scene07Cancellations() throws RideHailingException {
        scene(7, 11, 0, "Cancellations: within the 2-minute grace period vs after it");
        Ride freeCancel = rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.T_NAGAR,
                VehicleType.AUTO, 1, PaymentMethod.UPI);
        advanceTo(clock.now().plusMinutes(1));
        say("Priya's meeting got moved online - she cancels 1 minute after " + freeCancel.getCaptain().getName() + " was assigned.");
        Money fee = rideService.cancelByCustomer(freeCancel);
        say("  Fee charged: " + fee + " (grace period). " + ramesh.getName() + " is free again at " + ramesh.getLocation());

        advanceTo(at(11, 5));
        divyaWaitingRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.KILPAUK,
                VehicleType.BIKE, 1, PaymentMethod.UPI);
        Ride lateCancel = divyaWaitingRide;
        advanceTo(lateCancel.getExpectedArrivalAt());
        rideService.captainArrived(lateCancel);
        advanceTo(at(11, 8));
        say("Divya's friend offers her a lift, so she cancels 3 minutes after assignment (captain already waiting).");
        fee = rideService.cancelByCustomer(lateCancel);
        say("  Fee: " + fee + ". Outstanding on Divya's account: " + divya.getOutstandingFeeTotal()
                + " - it will appear on her NEXT ride.");
    }

    private void scene08WaitingCharge() throws RideHailingException {
        scene(8, 12, 30, "Captain arrives, waits 4 minutes (1 minute charged), then starts");
        divyaWaitingRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.KOYAMBEDU,
                VehicleType.AUTO, 1, PaymentMethod.UPI);
        advanceTo(divyaWaitingRide.getExpectedArrivalAt());
        rideService.captainArrived(divyaWaitingRide);
        say("Divya is still finishing lunch...");
        advanceTo(clock.now().plusMinutes(4));
        rideService.startRide(divyaWaitingRide, divyaWaitingRide.revealOtpTo(divya));
        say("Waited " + divyaWaitingRide.waitingMinutes() + " minutes: 3 free + 1 charged at "
                + FareService.WAITING_CHARGE_PER_MINUTE + "/min.");
        driveToCompletion(divyaWaitingRide);
        printReceipt(divyaWaitingRide);
        say("The " + divyaWaitingRide.getReceipt().getPreviousFeesTotal()
                + " late-cancellation fee was billed once. Outstanding now: " + divya.getOutstandingFeeTotal());
    }

    private void scene09NoShow() throws RideHailingException {
        scene(9, 13, 0, "Customer no-show");
        Ride noShow = rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.ADYAR,
                VehicleType.BIKE, 1, PaymentMethod.UPI);
        advanceTo(noShow.getExpectedArrivalAt());
        rideService.captainArrived(noShow);
        advanceTo(clock.now().plusMinutes(3));
        say("Karthik tries to cancel after 3 minutes...");
        try {
            rideService.cancelForNoShow(noShow);
        } catch (InvalidRideStatusException e) {
            say("  ✘ Not yet: " + e.getMessage());
        }
        advanceTo(clock.now().plusMinutes(2));
        Money fee = rideService.cancelForNoShow(noShow);
        say("No-show fee " + fee + " goes onto Priya's next ride. Karthik is free at " + karthik.getLocation() + ".");
    }

    private void scene10OtpAttempts() throws RideHailingException {
        scene(10, 14, 0, "OTP: one wrong attempt, then correct. Separately, 3 wrong attempts");
        lakshmiOtpRide = rideService.bookRide(lakshmi, lakshmi.getLocation(), ChennaiPlaces.KK_NAGAR,
                VehicleType.BIKE, 1, PaymentMethod.WALLET);
        advanceTo(lakshmiOtpRide.getExpectedArrivalAt());
        rideService.captainArrived(lakshmiOtpRide);
        advanceTo(clock.now().plusMinutes(1));
        try {
            rideService.startRide(lakshmiOtpRide, "1234");
        } catch (InvalidOtpException e) {
            say("  ✘ Selvi typed 1234: " + e.getMessage());
        }
        rideService.startRide(lakshmiOtpRide, lakshmiOtpRide.revealOtpTo(lakshmi));
        say("  ✔ Correct OTP - ride started.");
        driveToCompletion(lakshmiOtpRide);

        say("Meanwhile Priya books a bike at Guindy and reads out an old OTP from yesterday...");
        priyaOtpFailedRide = rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.VELACHERY,
                VehicleType.BIKE, 1, PaymentMethod.UPI);
        advanceTo(priyaOtpFailedRide.getExpectedArrivalAt());
        rideService.captainArrived(priyaOtpFailedRide);
        String[] wrongOtps = {"4821", "4812", "8421"};
        for (String wrong : wrongOtps) {
            try {
                rideService.startRide(priyaOtpFailedRide, wrong);
            } catch (InvalidOtpException e) {
                say("  ✘ " + e.getMessage());
            }
        }
        say("Ride status: " + priyaOtpFailedRide.getStatus() + " (" + priyaOtpFailedRide.getCancellationReason()
                + "), fee " + priyaOtpFailedRide.getCancellationFee() + ". Priya's no-show fee still outstanding: "
                + priya.getOutstandingFeeTotal());
        System.out.print(priyaOtpFailedRide.timelineAsText());
    }

    private void scene11CaptainCancelsAndRematch() throws RideHailingException {
        scene(11, 15, 30, "Captain accepts, then cancels - the ride is re-matched and the customer is not charged");
        arun.travelOnOwnTo(ChennaiPlaces.EGMORE);
        say("Arun took the suburban train back from Tambaram and is at " + arun.getLocation() + ".");
        arunRematchedRide = rideService.bookRide(arun, arun.getLocation(), ChennaiPlaces.MYLAPORE,
                VehicleType.AUTO, 1, PaymentMethod.UPI);
        advanceTo(clock.now().plusMinutes(1));
        say("Murugan notices a flat tyre and cancels.");
        Captain replacement = rideService.captainCancelsAfterAccepting(arunRematchedRide);
        say("  Re-matched to " + replacement.getName() + ". Murugan stays at " + murugan.getLocation()
                + ", cancellations after accepting: " + murugan.getCancellationsAfterAccepting());
        say("  Offer log: " + arunRematchedRide.getOfferLog());
        driveToCompletion(arunRematchedRide);
        say("Arun paid " + arunRematchedRide.getReceipt().getTotal() + " - no cancellation fee (the captain cancelled, not Arun).");
    }

    private void scene12ChangeDestination() throws RideHailingException {
        scene(12, 17, 45, "Change destination mid-ride - fare follows the actual route");
        lakshmiChangedRide = rideService.bookRide(lakshmi, lakshmi.getLocation(), ChennaiPlaces.PORUR,
                VehicleType.CAB_ECONOMY, 2, PaymentMethod.UPI);
        driveUntilStarted(lakshmiChangedRide);
        advanceTo(clock.now().plusMinutes(4));
        say("Lakshmi's sister calls: meet at Koyambedu instead of Porur.");
        Location changePoint = rideService.changeDestination(lakshmiChangedRide, ChennaiPlaces.KOYAMBEDU);
        say("  Change point: " + changePoint + ". Trying to change a second time...");
        try {
            rideService.changeDestination(lakshmiChangedRide, ChennaiPlaces.ANNA_NAGAR);
        } catch (InvalidRideStatusException e) {
            say("  ✘ " + e.getMessage());
        }
        driveToCompletion(lakshmiChangedRide);
        printReceipt(lakshmiChangedRide);
        say("Lakshmi is at " + lakshmi.getLocation() + ", Rajesh is at " + rajesh.getLocation() + ".");
    }

    private void scene13EndTripEarly() throws RideHailingException {
        scene(13, 18, 30, "End trip early - partial fare, both locations move to the stop point");
        arunEarlyRide = rideService.bookRide(arun, arun.getLocation(), ChennaiPlaces.SHOLINGANALLUR,
                VehicleType.AUTO, 1, PaymentMethod.WALLET);
        driveUntilStarted(arunEarlyRide);
        say("Expected at Sholinganallur by " + SimulatedClock.format(arunEarlyRide.getExpectedDropAt())
                + " - OMR is crawling at this hour.");
        advanceTo(clock.now().plusMinutes(25));
        say("Stuck in traffic, Arun decides to get off and take the metro.");
        FareReceipt receipt = rideService.endTripEarly(arunEarlyRide);
        System.out.println(receipt);
        say("Arun is at " + arun.getLocation() + ", Anbu is at " + anbu.getLocation() + " (same stop point).");
    }

    private void scene14PaymentPending() throws RideHailingException {
        scene(14, 19, 0, "Wallet too low, UPI fails, ride PAYMENT_PENDING, booking blocked, then cash");
        say("Harish has " + harish.getWalletBalance() + " in his wallet and books a cab paying by wallet.");
        harishPendingRide = rideService.bookRide(harish, harish.getLocation(), ChennaiPlaces.VELACHERY,
                VehicleType.CAB_ECONOMY, 1, PaymentMethod.WALLET);
        driveToDrop(harishPendingRide);
        say("Status: " + harishPendingRide.getStatus() + ", wallet still " + harish.getWalletBalance() + " (never partly deducted)");

        say("Harish retries with UPI (" + harish.getUpiId() + ")...");
        PaymentStatus upi = rideService.retryPayment(harishPendingRide, PaymentMethod.UPI);
        say("  UPI result: " + upi + ", ride is " + harishPendingRide.getStatus());

        say("Harish tries to book a bike home anyway...");
        try {
            rideService.bookRide(harish, harish.getLocation(), ChennaiPlaces.GUINDY, VehicleType.BIKE, 1);
        } catch (InvalidBookingException e) {
            say("  ✘ Rejected (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }

        advanceTo(clock.now().plusMinutes(2));
        say("Harish pays Senthil in cash.");
        rideService.retryPayment(harishPendingRide, PaymentMethod.CASH);
        say("  Ride is now " + harishPendingRide.getStatus() + ".");
        printReceipt(harishPendingRide);
        try {
            rideService.retryPayment(harishPendingRide, PaymentMethod.UPI);
        } catch (InvalidRideStatusException e) {
            say("  ✘ Paying again is refused: " + e.getMessage());
        }

        Ride bikeHome = rideService.bookRide(harish, harish.getLocation(), ChennaiPlaces.GUINDY, VehicleType.BIKE, 1);
        say("  ✔ Booking now succeeds: " + bikeHome.getId() + " with " + bikeHome.getCaptain().getName());
        autopilot(bikeHome);
    }

    private void scene15CashDuesBlock() throws RideHailingException {
        scene(15, 19, 50, "Cash dues cross ₹500 - captain blocked from cash rides, settles, unblocked");
        divya.travelOnOwnTo(ChennaiPlaces.EGMORE);
        say("Divya took the metro from Koyambedu to Egmore. Murugan owes " + murugan.getDuesOwed()
                + " (limit " + Captain.CASH_DUES_LIMIT + ").");
        advanceTo(at(20, 0));
        divyaCashRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.ANNA_NAGAR,
                VehicleType.AUTO, 1, PaymentMethod.CASH);
        driveToCompletion(divyaCashRide);
        say("Murugan collected " + divyaCashRide.getReceipt().getTotal() + " in cash. Dues now "
                + murugan.getDuesOwed() + " -> blocked for cash rides: " + murugan.isBlockedForCash());

        say("Divya now books a cash auto from Anna Nagar to Kilpauk. Murugan is right here, but...");
        say("  Murugan: " + reasonOrEligible(murugan, divya.getLocation(), VehicleType.AUTO, PaymentMethod.CASH));
        say("  Murugan for a UPI ride: " + reasonOrEligible(murugan, divya.getLocation(), VehicleType.AUTO, PaymentMethod.UPI));
        divyaSkippedMuruganRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.KILPAUK,
                VehicleType.AUTO, 1, PaymentMethod.CASH);
        say("  -> went to " + divyaSkippedMuruganRide.getCaptain().getName() + " instead.");
        autopilot(divyaSkippedMuruganRide);

        advanceTo(clock.now().plusMinutes(2));
        Money settled = captainService.settleDues(murugan);
        say("Murugan settles " + settled + " via UPI in the captain app. Blocked for cash: "
                + murugan.isBlockedForCash() + ". Cash rides: "
                + reasonOrEligible(murugan, ChennaiPlaces.ANNA_NAGAR, VehicleType.AUTO, PaymentMethod.CASH));
    }

    private void scene16NoPremiumAtSholinganallur() throws RideHailingException {
        scene(16, 21, 0, "No Cab Premium near Sholinganallur");
        arun.travelOnOwnTo(ChennaiPlaces.SHOLINGANALLUR);
        say("Arun finished a late meeting at his office in Sholinganallur and wants an SUV to T. Nagar.");
        for (FareEstimate estimate : rideService.getFareEstimates(arun.getLocation(), ChennaiPlaces.T_NAGAR)) {
            say("    " + estimate);
        }
        try {
            rideService.bookRide(arun, arun.getLocation(), ChennaiPlaces.T_NAGAR, VehicleType.CAB_PREMIUM, 3, PaymentMethod.UPI);
        } catch (NoCaptainAvailableException e) {
            say("  ✘ " + e.getMessage());
        }
        say("  Balaji: " + reasonOrEligible(balaji, arun.getLocation(), VehicleType.CAB_PREMIUM, PaymentMethod.UPI));
        say("  Vignesh: " + reasonOrEligible(vignesh, arun.getLocation(), VehicleType.CAB_PREMIUM, PaymentMethod.UPI));
    }

    private void scene17AirportPremiumNightCoupon() throws RideHailingException {
        scene(17, 23, 30, "Cab Premium, Chennai Airport -> Anna Nagar: night charge + FIRSTRIDE coupon, wallet");
        priya.travelOnOwnTo(ChennaiPlaces.CHENNAI_AIRPORT);
        say("Priya took the metro to the airport to receive her parents (5 people with luggage).");
        try {
            rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.ANNA_NAGAR, VehicleType.CAB_ECONOMY,
                    5, PaymentMethod.WALLET, FareService.FIRSTRIDE);
        } catch (InvalidBookingException e) {
            say("  ✘ Cab Economy: " + e.getMessage());
        }
        priyaAirportRide = rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.ANNA_NAGAR,
                VehicleType.CAB_PREMIUM, 5, PaymentMethod.WALLET, FareService.FIRSTRIDE);
        driveToCompletion(priyaAirportRide);
        printReceipt(priyaAirportRide);
        say("Priya's wallet: " + priya.getWalletBalance() + ". Using FIRSTRIDE again...");
        try {
            rideService.getFareEstimates(priya.getLocation(), ChennaiPlaces.KILPAUK);
            rideService.bookRide(priya, priya.getLocation(), ChennaiPlaces.KILPAUK, VehicleType.BIKE, 1,
                    PaymentMethod.UPI, FareService.FIRSTRIDE);
        } catch (InvalidBookingException e) {
            say("  ✘ " + e.getMessage());
        }
    }

    private void scene18OutsideServiceArea() throws RideHailingException {
        scene(18, 23, 53, "A booking outside the service area");
        say("Arun tries to book a cab from Sholinganallur to Mahabalipuram for a weekend trip.");
        try {
            rideService.bookRide(arun, arun.getLocation(), ChennaiPlaces.MAHABALIPURAM, VehicleType.CAB_ECONOMY,
                    2, PaymentMethod.UPI);
        } catch (InvalidBookingException e) {
            say("  ✘ Rejected (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private void scene19OfflineMidRide() throws RideHailingException {
        scene(19, 23, 54, "A captain tries to go offline mid-ride");
        divyaNightRide = rideService.bookRide(divya, divya.getLocation(), ChennaiPlaces.EGMORE,
                VehicleType.AUTO, 1, PaymentMethod.UPI);
        say("Divya heads to Egmore station for her 12:30 AM train.");
        driveUntilStarted(divyaNightRide);
        Captain captain = divyaNightRide.getCaptain();
        advanceTo(clock.now().plusMinutes(2));
        say(captain.getName() + " wants to finish for the day and taps 'Go offline'...");
        try {
            captainService.goOffline(captain);
        } catch (CaptainBusyException e) {
            say("  ✘ " + e.getMessage());
        }
        driveToCompletion(divyaNightRide);
        captainService.goOffline(captain);
        say("  ✔ After dropping Divya, " + captain.getName() + " is " + captain.getStatus().getLabel()
                + " at " + captain.getLocation() + ".");
    }

    private void scene20Ratings() throws RideHailingException {
        scene(20, "Ratings");
        rateBothWays(priyaMorningRide, 5, 5);
        rateBothWays(divyaCentralRide, 4, 5);
        rateBothWays(arunTambaramRide, 5, 5);
        rateBothWays(divyaEgmoreRide, 4, 5);
        rateBothWays(harishChromepetRide, 5, 4);
        rateBothWays(lakshmiVadapalaniRide, 5, 5);
        rateBothWays(lakshmiOtpRide, 4, 5);
        rateBothWays(arunRematchedRide, 4, 5);
        rateBothWays(lakshmiChangedRide, 4, 5);
        rateBothWays(arunEarlyRide, 3, 5);
        rateBothWays(harishPendingRide, 4, 3);
        rateBothWays(divyaCashRide, 4, 5);
        rateBothWays(priyaAirportRide, 5, 5);
        say("Divya rates Ramesh 3★ for the Koyambedu ride (rash driving near CMBT).");
        rateBothWays(divyaWaitingRide, 3, 4);
        say("  Ramesh now has " + ramesh.getRatingCount() + " ratings, average "
                + String.format("%.2f", ramesh.getAverageRating()) + " -> flagged: " + ramesh.isFlagged());
        rateBothWays(divyaSkippedMuruganRide, 3, 5);
        rateBothWays(divyaNightRide, 4, 5);

        say("Priya tries to rate the ride that was auto-cancelled after 3 wrong OTPs...");
        try {
            ratingService.rateCaptain(priyaOtpFailedRide, 1);
        } catch (InvalidRideStatusException e) {
            say("  ✘ " + e.getMessage());
        }
        say("Priya tries to rate Karthik a second time for the morning ride...");
        try {
            ratingService.rateCaptain(priyaMorningRide, 5);
        } catch (InvalidRideStatusException e) {
            say("  ✘ " + e.getMessage());
        }
        for (Captain flagged : ratingService.flaggedCaptains()) {
            say("  ⚑ FLAGGED for review: " + flagged.getName() + String.format(" (%.2f★ over %d ratings)",
                    flagged.getAverageRating(), flagged.getRatingCount()));
        }
    }

    private void scene21EndOfDay() {
        scene(21, "End of day");

        System.out.println();
        System.out.println("  CAPTAIN EARNINGS - " + DEMO_DAY);
        System.out.println(String.format("  %-9s %-12s %5s %11s %11s %11s %11s %11s %11s",
                "Captain", "Vehicle", "Rides", "Gross", "Commission", "Net", "Cash coll.", "Dues added", "Dues now"));
        for (Captain captain : captainService.getAllCaptains()) {
            EarningsService.DayEarnings day = earningsService.getDayEarnings(captain, DEMO_DAY);
            System.out.println(String.format("  %-9s %-12s %5d %11s %11s %11s %11s %11s %11s",
                    captain.getName(), captain.getVehicleType(), day.getRides(), day.getGrossFare(),
                    day.getCommission(), day.getNetEarnings(), day.getCashCollected(), day.getDuesAdded(),
                    captain.getDuesOwed()));
        }

        System.out.println();
        System.out.println("  RATINGS LEADERBOARD");
        int rank = 1;
        for (Captain captain : ratingService.leaderboard()) {
            System.out.println(String.format("  %2d. %-9s %.2f★ (%d ratings)  acceptance %.0f%%  cancellations %d%s",
                    rank++, captain.getName(), captain.getAverageRating(), captain.getRatingCount(),
                    captain.getAcceptanceRate(), captain.getCancellationsAfterAccepting(),
                    captain.isFlagged() ? "  ⚑ FLAGGED" : ""));
        }

        System.out.println();
        System.out.println("  CUSTOMER RIDE HISTORY (chronological, cancelled rides included)");
        for (Customer customer : customerService.getAllCustomers()) {
            System.out.println(String.format("  %s - now at %s, wallet %s, rating %.2f★",
                    customer.getName(), customer.getLocation(), customer.getWalletBalance(), customer.getAverageRating()));
            for (Ride ride : customer.getRideHistory()) {
                String amount = ride.getReceipt() != null ? ride.getReceipt().getTotal().toString()
                        : (ride.getCancellationFee().isZero() ? "-" : "fee " + ride.getCancellationFee());
                String why = ride.getStatus() == RideStatus.CANCELLED ? "  (" + ride.getCancellationReason() + ")" : "";
                System.out.println(String.format("     %s  %s  %-15s %-12s %-44s %10s%s",
                        SimulatedClock.format(ride.getRequestedAt()), ride.getId(), ride.getStatus(),
                        ride.getVehicleType(), ride.getPickup() + " -> " + ride.getDrop(), amount, why));
            }
        }

        printBoard("Final Captain Position Board");

        System.out.println();
        System.out.println("  MONEY RECONCILIATION (" + earningsService.getPaidRides() + " paid rides)");
        System.out.println(String.format("    Paid by customers        %12s", earningsService.getTotalPaidByCustomers()));
        System.out.println(String.format("      = Captain earnings     %12s", earningsService.getTotalCaptainEarnings()));
        System.out.println(String.format("      + Platform commission  %12s", earningsService.getTotalCommission()));
        System.out.println(String.format("      + GST collected        %12s", earningsService.getTotalGst()));
        Money rightSide = earningsService.getTotalCaptainEarnings().plus(earningsService.getTotalCommission())
                .plus(earningsService.getTotalGst());
        System.out.println(String.format("      = %22s", rightSide));
        System.out.println(String.format("    Collected: cash %s + digital %s = %s",
                earningsService.getTotalCash(), earningsService.getTotalDigital(),
                earningsService.getTotalCash().plus(earningsService.getTotalDigital())));
        System.out.println(earningsService.isBalanced() ? "    Books Balanced ✔" : "    BOOKS DO NOT BALANCE ✘");
    }

    // ====================================================================== autopilot

    // The steps themselves live in RideAutopilot (shared with the interactive mode).

    private void autopilot(Ride ride) {
        autopilot.add(ride);
    }

    /** Moves the clock forward, running every autopilot step that falls due on the way, in time order. */
    private void advanceTo(LocalDateTime target) {
        autopilot.advanceTo(target);   // throws if a scene tries to go back in time
    }

    private void driveToCompletion(Ride ride) {
        autopilot.driveToCompletion(ride);
    }

    private void driveUntilStarted(Ride ride) {
        autopilot.driveUntilStarted(ride);
    }

    /** Drives up to the drop and finishes the trip, without the autopilot's automatic payment retry. */
    private void driveToDrop(Ride ride) throws RideHailingException {
        driveUntilStarted(ride);
        advanceTo(ride.getExpectedDropAt());
        rideService.completeTrip(ride);
    }

    // ====================================================================== output helpers

    private void banner() {
        System.out.println("════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  CHENNAI RIDE APP - one simulated day, " + DEMO_DAY + " (" + DEMO_DAY.getDayOfWeek() + ")");
        System.out.println("  Service area: " + chennai);
        System.out.println("════════════════════════════════════════════════════════════════════════════════════");
    }

    private void scene(int number, int hour, int minute, String title) {
        scene(number, at(hour, minute), title);
    }

    private void scene(int number, LocalDateTime start, String title) {
        advanceTo(start);
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────────────────────────────");
        System.out.println(String.format("  SCENE %d · %s · %s", number, SimulatedClock.format(clock.now()), title));
        System.out.println("────────────────────────────────────────────────────────────────────────────────────");
    }

    /** A scene without a fixed time: it starts when the previous one ended. */
    private void scene(int number, String title) {
        scene(number, clock.now(), title);
    }

    private void say(String text) {
        System.out.println(clock.stamp() + "  " + text);
    }

    private void printReceipt(Ride ride) {
        System.out.println(ride.getReceipt());
        System.out.println("  Paid with: " + (ride.getPaidWith() == null ? "not paid yet" : ride.getPaidWith())
                + " · status " + ride.getStatus());
    }

    private void printBoard(String title) {
        System.out.println();
        System.out.println(clock.stamp() + "  " + title);
        System.out.println(String.format("    %-9s %-26s %-22s %-14s %5s %11s %7s",
                "Captain", "Vehicle", "Location", "Status", "Rides", "Earnings", "Accept"));
        for (Captain captain : captainService.getAllCaptains()) {
            System.out.println(String.format("    %-9s %-26s %-22s %-14s %5d %11s %6.0f%%",
                    captain.getName(),
                    captain.getVehicleType() + " " + captain.getVehicle().getRegistrationNumber(),
                    captain.getLocation(), captain.getStatus().getLabel(), captain.getRidesCompleted(),
                    earningsService.getTotalNetEarnings(captain), captain.getAcceptanceRate()));
        }
        System.out.println();
    }

    private void rateBothWays(Ride ride, int customerGives, int captainGives) throws InvalidRideStatusException {
        ratingService.rateCaptain(ride, customerGives);
        ratingService.rateCustomer(ride, captainGives);
        say(String.format("  %s: %s -> %s %d★, %s -> %s %d★", ride.getId(), ride.getCustomer().getName(),
                ride.getCaptain().getName(), customerGives, ride.getCaptain().getName(),
                ride.getCustomer().getName(), captainGives));
    }

    private String reasonOrEligible(Captain captain, Location pickup, VehicleType type, PaymentMethod method) {
        String reason = matchingService.ineligibilityReason(captain, pickup, type, method, null, clock.now());
        return reason == null ? "eligible" : "not eligible - " + reason;
    }

    private static String km(Location a, Location b) {
        return String.format("%.1f km", a.distanceTo(b));
    }

    private static LocalDateTime at(int hour, int minute) {
        return ChennaiWorld.at(hour, minute);
    }

    // ====================================================================== for SelfCheck

    public EarningsService getEarningsService() {
        return earningsService;
    }

    public RideService getRideService() {
        return rideService;
    }

    public CaptainService getCaptainService() {
        return captainService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public SimulatedClock getClock() {
        return clock;
    }
}
