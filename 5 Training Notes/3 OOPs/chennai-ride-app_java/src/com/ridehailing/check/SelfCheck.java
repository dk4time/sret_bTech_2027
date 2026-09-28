package com.ridehailing.check;

import com.ridehailing.app.ChennaiRideApp;
import com.ridehailing.exception.ActiveRideExistsException;
import com.ridehailing.exception.CaptainBusyException;
import com.ridehailing.exception.CaptainNotEligibleException;
import com.ridehailing.exception.InsufficientWalletBalanceException;
import com.ridehailing.exception.InvalidBookingException;
import com.ridehailing.exception.InvalidOtpException;
import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.exception.NoCaptainAvailableException;
import com.ridehailing.exception.OutOfServiceAreaException;
import com.ridehailing.exception.PaymentPendingException;
import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.common.ServiceArea;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.payment.PaymentStatus;
import com.ridehailing.model.ride.CancellationReason;
import com.ridehailing.model.ride.FareEstimate;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideRequest;
import com.ridehailing.model.ride.RideStatus;
import com.ridehailing.model.ride.TimelineEntry;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.CaptainStatus;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.Auto;
import com.ridehailing.model.vehicle.Bike;
import com.ridehailing.model.vehicle.CabEconomy;
import com.ridehailing.model.vehicle.CabPremium;
import com.ridehailing.model.vehicle.Vehicle;
import com.ridehailing.model.vehicle.VehicleType;
import com.ridehailing.service.CaptainService;
import com.ridehailing.service.CustomerService;
import com.ridehailing.service.EarningsService;
import com.ridehailing.service.FareService;
import com.ridehailing.service.MatchingService;
import com.ridehailing.service.PaymentService;
import com.ridehailing.service.RatingService;
import com.ridehailing.service.RideService;
import com.ridehailing.time.SimulatedClock;

import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-Java self check (no JUnit). Each check prints PASS or FAIL; the last line is the score.
 * Run: java -cp out com.ridehailing.check.SelfCheck
 */
public class SelfCheck {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 14);

    private static int passed;
    private static int failed;

    /** Something that may throw - used to check that an illegal action is refused. */
    private interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (!runAll()) {
            System.exit(1);
        }
    }

    /** Runs every check and prints the score. Returns true when all checks pass (used by the start-up menu). */
    public static boolean runAll() throws Exception {
        passed = 0;
        failed = 0;
        System.out.println("SelfCheck - Chennai Ride App");
        section("1. Fares");
        checkFares();
        section("2. Ride status transitions");
        checkValidTransitions();
        checkInvalidTransitions();
        section("3. OTP");
        checkOtpRule();
        section("4. Location continuity");
        checkLocationContinuity();
        section("5. Matching eligibility");
        checkMatching();
        section("6. Canonical Egmore -> Tambaram");
        checkCanonicalCase();
        section("7. Cancellations and fees");
        checkCancellations();
        section("8. Booking rules");
        checkBookingRules();
        section("9. Payments");
        checkPayments();
        section("10. Cash dues limit");
        checkCashDues();
        section("11. Clock and captain timing");
        checkClock();
        section("12. Captains, ratings, value objects");
        checkCaptainsAndRatings();
        section("13. Full demo day: timing and money reconciliation");
        checkFullDay();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed, " + (passed + failed) + " checks - "
                + (failed == 0 ? "100% PASS" : "FAILURES PRESENT"));
        return failed == 0;
    }

    // ====================================================================== 1. fares

    private static void checkFares() throws Exception {
        Vehicle bike = new Bike("TN-01-SC-0001", "Honda Activa");
        Vehicle auto = new Auto("TN-01-SC-0002", "Bajaj RE Auto");
        Vehicle economy = new CabEconomy("TN-01-SC-0003", "Maruti Dzire");
        Vehicle premium = new CabPremium("TN-01-SC-0004", "Toyota Innova Crysta");

        check("Bike 10 km / 20 min = 15 + 50 + 10 = ₹75", bike.calculateBaseFare(10, 20).equals(Money.of(75)));
        check("Auto 10 km / 20 min = 25 + 110 + 20 = ₹155", auto.calculateBaseFare(10, 20).equals(Money.of(155)));
        check("Cab Economy 10 km / 20 min = 50 + 140 + 30 = ₹220", economy.calculateBaseFare(10, 20).equals(Money.of(220)));
        check("Cab Premium 10 km / 20 min = 80 + 200 + 40 = ₹320", premium.calculateBaseFare(10, 20).equals(Money.of(320)));
        check("Bike < Auto < Cab Economy < Cab Premium",
                bike.calculateBaseFare(5, 10).isLessThan(auto.calculateBaseFare(5, 10))
                        && auto.calculateBaseFare(5, 10).isLessThan(economy.calculateBaseFare(5, 10))
                        && economy.calculateBaseFare(5, 10).isLessThan(premium.calculateBaseFare(5, 10)));
        check("Minimum fare applied: Bike 0.6 km / 1 min = ₹25 (raw ₹18.50)", bike.calculateBaseFare(0.6, 1).equals(Money.of(25)));
        check("Minimum fare applied: Auto ₹35, Economy ₹80, Premium ₹150",
                auto.calculateBaseFare(0.6, 1).equals(Money.of(35)) && economy.calculateBaseFare(0.6, 1).equals(Money.of(80))
                        && premium.calculateBaseFare(0.6, 1).equals(Money.of(150)));
        check("Seats: Bike 1, Auto 3, Economy 4, Premium 6 (vehicle matches its enum)",
                bike.seatCapacity() == 1 && auto.seatCapacity() == 3 && economy.seatCapacity() == 4 && premium.seatCapacity() == 6
                        && bike.seatCapacity() == VehicleType.BIKE.getSeatCapacity()
                        && premium.seatCapacity() == VehicleType.CAB_PREMIUM.getSeatCapacity());

        check("Night window: 10:59 PM is NOT night", !FareService.isNightTime(at(22, 59)));
        check("Night window: 11:00 PM IS night", FareService.isNightTime(at(23, 0)));
        check("Night window: 4:59 AM is night, 5:00 AM is not",
                FareService.isNightTime(at(4, 59)) && !FareService.isNightTime(at(5, 0)));

        // Real rides that START at 10:59 PM vs 11:00 PM
        FareReceipt before = rideStartingAt(22, 59);
        FareReceipt after = rideStartingAt(23, 0);
        check("Ride starting 10:59 PM has no night charge", before.getNightCharge().isZero());
        check("Ride starting 11:00 PM has a +20% night charge",
                !after.getNightCharge().isZero()
                        && after.getNightCharge().equals(after.getBaseFare().plus(after.getDistanceCharge())
                        .plus(after.getTimeCharge()).plus(after.getMinimumFareTopUp()).plus(after.getSurgeCharge()).percent(20)));
        check("GST is 5% of the subtotal and total = subtotal + GST",
                after.getGst().equals(after.getSubtotal().percent(5)) && after.getTotal().equals(after.getSubtotal().plus(after.getGst())));

        World w = new World(8, 30);
        FareService fares = w.fares;
        BigDecimal hotspotPeak = fares.calculateSurge(ChennaiPlaces.CHENNAI_CENTRAL, at(9, 0), 0, 3);
        BigDecimal quiet = fares.calculateSurge(ChennaiPlaces.ADYAR, at(14, 0), 0, 3);
        BigDecimal crazy = fares.calculateSurge(ChennaiPlaces.KOYAMBEDU, at(18, 0), 40, 1);
        check("Surge: hotspot at peak (1.4x) > off-peak with enough captains (1.0x)",
                hotspotPeak.compareTo(new BigDecimal("1.4")) == 0 && quiet.compareTo(BigDecimal.ONE) == 0);
        check("Surge is capped at 2.0x", crazy.compareTo(new BigDecimal("2.0")) == 0);

        Customer c = w.customer("Estimator", ChennaiPlaces.VELACHERY, Money.of(100));
        w.captain("Bike Near", ChennaiPlaces.VELACHERY, new Bike(w.plate(), "Honda Activa"));
        List<FareEstimate> estimates = w.rides.getFareEstimates(c.getLocation(), ChennaiPlaces.GUINDY);
        check("Estimates are returned for all 4 vehicle types", estimates.size() == 4);
        check("Estimate shows availability: bike available, premium not",
                estimates.get(0).isCaptainAvailable() && estimates.get(0).getNearestCaptainEtaMinutes() >= 1
                        && !estimates.get(3).isCaptainAvailable());

        // Coupon never takes the fare below the minimum fare; coupon needs a ₹100 fare
        World cw = new World(14, 0);
        Customer couponUser = cw.customer("Coupon", ChennaiPlaces.GUINDY, Money.of(500));
        cw.captain("Auto Guindy", ChennaiPlaces.GUINDY, new Auto(cw.plate(), "TVS King"));
        Ride small = cw.rides.bookRide(couponUser, couponUser.getLocation(), ChennaiPlaces.T_NAGAR, VehicleType.AUTO, 1,
                PaymentMethod.UPI, FareService.FIRSTRIDE);
        FareReceipt smallReceipt = cw.completeNormally(small);
        check("Coupon not applied when trip fare < ₹100 (and stays unused)",
                smallReceipt.getCouponDiscount().isZero() && !couponUser.hasUsedCoupon(FareService.FIRSTRIDE));
        cw.captain("Premium T Nagar", couponUser.getLocation(), new CabPremium(cw.plate(), "Toyota Innova Crysta"));
        Ride atMinimum = cw.rides.bookRide(couponUser, couponUser.getLocation(), ChennaiPlaces.ASHOK_NAGAR,
                VehicleType.CAB_PREMIUM, 2, PaymentMethod.UPI, FareService.FIRSTRIDE);
        FareReceipt minimumReceipt = cw.completeNormally(atMinimum);
        check("Coupon never takes the fare below the minimum fare (₹150 premium trip gets ₹0 off)",
                minimumReceipt.getTripFare().equals(Money.of(150)) && minimumReceipt.getCouponDiscount().isZero()
                        && minimumReceipt.getRideAmount().equals(Money.of(150)) && !couponUser.hasUsedCoupon(FareService.FIRSTRIDE));
        Ride longer = cw.rides.bookRide(couponUser, couponUser.getLocation(), ChennaiPlaces.EGMORE,
                VehicleType.CAB_PREMIUM, 2, PaymentMethod.UPI, FareService.FIRSTRIDE);
        FareReceipt couponReceipt = cw.completeNormally(longer);
        check("FIRSTRIDE gives ₹50 off a fare well above the minimum, then counts as used",
                couponReceipt.getCouponDiscount().equals(Money.of(50))
                        && couponReceipt.getRideAmount().equals(couponReceipt.getTripFare().minus(Money.of(50)))
                        && couponUser.hasUsedCoupon(FareService.FIRSTRIDE));
        check("FIRSTRIDE is once per customer", throwsA(InvalidBookingException.class, () -> cw.rides.bookRide(couponUser,
                couponUser.getLocation(), ChennaiPlaces.GUINDY, VehicleType.CAB_PREMIUM, 1, PaymentMethod.UPI, FareService.FIRSTRIDE)));

        // The captain's drive to the pickup is never billed.
        World tw = new World(14, 0);
        Customer rider = tw.customer("Rider", ChennaiPlaces.GUINDY, Money.of(500));
        tw.captain("Far Bike", ChennaiPlaces.VELACHERY, new Bike(tw.plate(), "TVS Jupiter"));
        Ride trip = tw.rides.bookRide(rider, rider.getLocation(), ChennaiPlaces.ADYAR, VehicleType.BIKE, 1, PaymentMethod.UPI);
        tw.completeNormally(trip);
        check("Billed distance is pickup -> drop only (captain's 2.8 km approach not billed)",
                Math.abs(trip.actualDistanceKm() - ChennaiPlaces.GUINDY.distanceTo(ChennaiPlaces.ADYAR)) < 0.001);
        check("Final fare equals the estimate when the trip goes exactly as estimated",
                trip.getReceipt().getTotal().equals(trip.getEstimate().getTotalFare()));
    }

    private static FareReceipt rideStartingAt(int hour, int minute) throws Exception {
        // book 2 minutes before the start time so the captain can arrive first
        World early = new World(LocalDateTime.of(DAY, LocalTime.of(hour, minute)).minusMinutes(2));
        Customer c = early.customer("Night Owl", ChennaiPlaces.EGMORE, Money.of(1000));
        early.captain("Night Bike", ChennaiPlaces.EGMORE, new Bike(early.plate(), "Honda Activa"));
        Ride ride = early.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KILPAUK, VehicleType.BIKE, 1, PaymentMethod.UPI);
        early.clock.advanceTo(ride.getExpectedArrivalAt());
        early.rides.captainArrived(ride);
        early.clock.advanceTo(LocalDateTime.of(DAY, LocalTime.of(hour, minute)));
        early.rides.startRide(ride, ride.revealOtpTo(c));
        early.clock.advanceTo(ride.getExpectedDropAt());
        return early.rides.completeTrip(ride);
    }

    // ====================================================================== 2. transitions

    private static void checkValidTransitions() throws Exception {
        World w = new World(12, 0);
        Customer c = w.customer("Valid", ChennaiPlaces.MYLAPORE, Money.of(1000));
        w.captain("Valid Auto", ChennaiPlaces.MYLAPORE, new Auto(w.plate(), "Bajaj RE Auto"));
        Ride ride = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ADYAR, VehicleType.AUTO, 1, PaymentMethod.UPI);
        check("REQUESTED -> CAPTAIN_ASSIGNED on booking", ride.getStatus() == RideStatus.CAPTAIN_ASSIGNED);
        w.clock.advanceTo(ride.getExpectedArrivalAt());
        w.rides.captainArrived(ride);
        check("CAPTAIN_ASSIGNED -> CAPTAIN_ARRIVED", ride.getStatus() == RideStatus.CAPTAIN_ARRIVED);
        w.rides.startRide(ride, ride.revealOtpTo(c));
        check("CAPTAIN_ARRIVED -> IN_PROGRESS with the right OTP", ride.getStatus() == RideStatus.IN_PROGRESS);
        w.clock.advanceTo(ride.getExpectedDropAt());
        w.rides.completeTrip(ride);
        check("IN_PROGRESS -> COMPLETED after drop and payment", ride.getStatus() == RideStatus.COMPLETED);
        check("Every status change is on the timeline with a non-decreasing timestamp", timelineOrdered(ride)
                && ride.getTimeline().size() >= 5);

        // REQUESTED -> CANCELLED (a request not yet matched)
        Ride pending = new Ride(new RideRequest(c, c.getLocation(), ChennaiPlaces.GUINDY, VehicleType.BIKE, 1,
                PaymentMethod.UPI, null, w.clock.now()), "1234", BigDecimal.ONE,
                w.fares.estimate(VehicleType.BIKE, c.getLocation(), ChennaiPlaces.GUINDY, w.clock.now(), BigDecimal.ONE, 3));
        Money fee = w.rides.cancelByCustomer(pending);
        check("REQUESTED -> CANCELLED is free", pending.getStatus() == RideStatus.CANCELLED && fee.isZero());

        // CAPTAIN_ASSIGNED -> REQUESTED -> CAPTAIN_ASSIGNED (captain cancels, re-matched)
        World r = new World(15, 30);
        Customer rc = r.customer("Rematch", ChennaiPlaces.EGMORE, Money.of(500));
        Captain first = r.captain("First Auto", ChennaiPlaces.EGMORE, new Auto(r.plate(), "Bajaj RE Auto"));
        Captain second = r.captain("Second Auto", ChennaiPlaces.KILPAUK, new Auto(r.plate(), "TVS King"));
        Ride rematch = r.rides.bookRide(rc, rc.getLocation(), ChennaiPlaces.MYLAPORE, VehicleType.AUTO, 1, PaymentMethod.UPI);
        Captain replacement = r.rides.captainCancelsAfterAccepting(rematch);
        check("Captain cancel -> back to matching -> CAPTAIN_ASSIGNED to another captain",
                rematch.getStatus() == RideStatus.CAPTAIN_ASSIGNED && replacement.equals(second) && !replacement.equals(first));
        check("Captain who cancelled is counted, free, and stays where he was",
                first.getCancellationsAfterAccepting() == 1 && first.getStatus() == CaptainStatus.AVAILABLE
                        && first.getLocation().equals(ChennaiPlaces.EGMORE));
        check("Captain who cancelled THIS ride is never re-offered it",
                r.matching.ineligibilityReason(first, rc.getLocation(), VehicleType.AUTO, PaymentMethod.UPI, rematch,
                        r.clock.now()) != null);
        check("Customer is not charged when the captain cancels",
                rc.getOutstandingFeeTotal().isZero() && rematch.getCancellationFee().isZero());

        // IN_PROGRESS -> PAYMENT_PENDING -> COMPLETED
        World p = new World(19, 0);
        Customer pc = p.customer("Pending", ChennaiPlaces.VELACHERY, Money.of(10), "pending@okhdfcbank");
        p.captain("Pending Cab", ChennaiPlaces.VELACHERY, new CabEconomy(p.plate(), "Maruti Dzire"));
        Ride pr = p.rides.bookRide(pc, pc.getLocation(), ChennaiPlaces.GUINDY, VehicleType.CAB_ECONOMY, 1, PaymentMethod.WALLET);
        p.completeNormally(pr);
        check("IN_PROGRESS -> PAYMENT_PENDING when payment fails", pr.getStatus() == RideStatus.PAYMENT_PENDING);
        p.rides.retryPayment(pr, PaymentMethod.CASH);
        check("PAYMENT_PENDING -> COMPLETED after cash", pr.getStatus() == RideStatus.COMPLETED);
    }

    private static void checkInvalidTransitions() throws Exception {
        World w = new World(12, 0);
        Customer c = w.customer("Invalid", ChennaiPlaces.MYLAPORE, Money.of(2000));
        w.captain("Invalid Auto", ChennaiPlaces.ROYAPETTAH, new Auto(w.plate(), "Bajaj RE Auto"));

        Ride assigned = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ADYAR, VehicleType.AUTO, 1, PaymentMethod.UPI);
        check("Captain cannot arrive before the ETA (no teleporting)",
                throwsA(InvalidRideStatusException.class, () -> w.rides.captainArrived(assigned)));
        check("Cannot start a ride that is only CAPTAIN_ASSIGNED",
                throwsA(InvalidRideStatusException.class, () -> w.rides.startRide(assigned, assigned.revealOtpTo(c))));
        check("Cannot complete a ride that never started",
                throwsA(InvalidRideStatusException.class, () -> w.rides.completeTrip(assigned)));
        check("Cannot end early a ride that never started",
                throwsA(InvalidRideStatusException.class, () -> w.rides.endTripEarly(assigned)));
        check("Cannot change destination before the ride starts",
                throwsA(InvalidRideStatusException.class, () -> w.rides.changeDestination(assigned, ChennaiPlaces.GUINDY)));
        check("Cannot assign a second captain to an assigned ride",
                throwsA(InvalidRideStatusException.class, () -> assigned.assignCaptain(assigned.getCaptain(), w.clock.now(), w.clock.now())));
        check("Cannot rate a ride that is not completed",
                throwsA(InvalidRideStatusException.class, () -> w.ratings.rateCaptain(assigned, 5)));

        w.clock.advanceTo(assigned.getExpectedArrivalAt());
        w.rides.captainArrived(assigned);
        check("Captain cannot 'arrive' twice",
                throwsA(InvalidRideStatusException.class, () -> w.rides.captainArrived(assigned)));
        w.rides.startRide(assigned, assigned.revealOtpTo(c));
        check("Cannot start a ride that is already IN_PROGRESS",
                throwsA(InvalidRideStatusException.class, () -> w.rides.startRide(assigned, assigned.revealOtpTo(c))));
        check("Cannot cancel a ride that is IN_PROGRESS (only end early)",
                throwsA(InvalidRideStatusException.class, () -> w.rides.cancelByCustomer(assigned)));
        check("Cannot reach the destination before the travel time has passed",
                throwsA(InvalidRideStatusException.class, () -> w.rides.completeTrip(assigned)));
        w.clock.advanceTo(assigned.getExpectedDropAt());
        w.rides.completeTrip(assigned);
        check("Cannot cancel a COMPLETED ride",
                throwsA(InvalidRideStatusException.class, () -> w.rides.cancelByCustomer(assigned)));
        check("Cannot complete (pay) a COMPLETED ride again",
                throwsA(InvalidRideStatusException.class, () -> assigned.complete(PaymentMethod.CASH, w.clock.now())));
        check("Cannot end early a COMPLETED ride",
                throwsA(InvalidRideStatusException.class, () -> w.rides.endTripEarly(assigned)));

        Ride cancelled = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.MYLAPORE.equals(c.getLocation())
                ? ChennaiPlaces.GUINDY : ChennaiPlaces.MYLAPORE, VehicleType.AUTO, 1, PaymentMethod.UPI);
        w.rides.cancelByCustomer(cancelled);
        check("Cannot start a CANCELLED ride",
                throwsA(InvalidRideStatusException.class, () -> w.rides.startRide(cancelled, cancelled.revealOtpTo(c))));
        check("Cannot mark a CANCELLED ride arrived",
                throwsA(InvalidRideStatusException.class, () -> w.rides.captainArrived(cancelled)));
        check("Cannot cancel a CANCELLED ride twice",
                throwsA(InvalidRideStatusException.class, () -> w.rides.cancelByCustomer(cancelled)));
        check("Cannot rate a CANCELLED ride",
                throwsA(InvalidRideStatusException.class, () -> w.ratings.rateCaptain(cancelled, 4)));
        check("Cannot retry payment on a ride that is not PAYMENT_PENDING",
                throwsA(InvalidRideStatusException.class, () -> w.rides.retryPayment(cancelled, PaymentMethod.UPI)));
        check("A party cannot use another party's cancellation reason",
                throwsA(InvalidRideStatusException.class, () -> new Ride(new RideRequest(c, c.getLocation(),
                        ChennaiPlaces.GUINDY, VehicleType.BIKE, 1, PaymentMethod.UPI, null, w.clock.now()), "1111",
                        BigDecimal.ONE, cancelled.getEstimate()).cancel(CancellationReason.CUSTOMER_NO_SHOW,
                        CancellationReason.Party.CUSTOMER, Money.ZERO, w.clock.now())));
    }

    // ====================================================================== 3. OTP

    private static void checkOtpRule() throws Exception {
        World w = new World(14, 0);
        Customer c = w.customer("Otp", ChennaiPlaces.VADAPALANI, Money.of(500));
        Captain bike = w.captain("Otp Bike", ChennaiPlaces.VADAPALANI, new Bike(w.plate(), "TVS Jupiter"));
        Ride ok = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KK_NAGAR, VehicleType.BIKE, 1, PaymentMethod.UPI);
        w.arrive(ok);
        String otp = ok.revealOtpTo(c);
        check("OTP is 4 digits and only its customer can see it",
                otp.matches("\\d{4}") && throwsA(IllegalArgumentException.class, () -> ok.revealOtpTo(
                        w.customer("Stranger", ChennaiPlaces.VADAPALANI, Money.ZERO))));
        String wrong = otp.equals("0000") ? "1111" : "0000";
        check("1st wrong OTP throws InvalidOtpException (2 left)", otpAttemptsLeft(w, ok, wrong) == 2);
        check("2nd wrong OTP throws InvalidOtpException (1 left)", otpAttemptsLeft(w, ok, wrong) == 1);
        w.rides.startRide(ok, otp);
        check("Correct OTP after 2 wrong ones still starts the ride", ok.getStatus() == RideStatus.IN_PROGRESS);
        w.clock.advanceTo(ok.getExpectedDropAt());
        w.rides.completeTrip(ok);

        Ride bad = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.VADAPALANI, VehicleType.BIKE, 1, PaymentMethod.UPI);
        w.arrive(bad);
        String badWrong = bad.revealOtpTo(c).equals("0000") ? "1111" : "0000";
        otpAttemptsLeft(w, bad, badWrong);
        otpAttemptsLeft(w, bad, badWrong);
        int left = otpAttemptsLeft(w, bad, badWrong);
        check("3rd wrong OTP auto-cancels the ride as OTP_FAILED",
                left == 0 && bad.getStatus() == RideStatus.CANCELLED && bad.getCancellationReason() == CancellationReason.OTP_FAILED);
        check("OTP_FAILED has no fee and the captain is free again",
                bad.getCancellationFee().isZero() && c.getOutstandingFeeTotal().isZero()
                        && bike.getStatus() == CaptainStatus.AVAILABLE);
        check("After auto-cancel, even the right OTP cannot start it",
                throwsA(InvalidRideStatusException.class, () -> w.rides.startRide(bad, bad.revealOtpTo(c))));
    }

    private static int otpAttemptsLeft(World w, Ride ride, String otp) throws Exception {
        try {
            w.rides.startRide(ride, otp);
            return -1;
        } catch (InvalidOtpException e) {
            return e.getAttemptsLeft();
        }
    }

    // ====================================================================== 4. locations

    private static void checkLocationContinuity() throws Exception {
        World w = new World(14, 0);
        Customer c = w.customer("Mover", ChennaiPlaces.GUINDY, Money.of(2000));
        Captain cab = w.captain("Mover Cab", ChennaiPlaces.GUINDY, new CabEconomy(w.plate(), "Hyundai Aura"));

        Ride normal = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ADYAR, VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
        w.arrive(normal);
        check("On arrival the captain is within 100 m of the pickup",
                cab.getLocation().isWithinMeters(normal.getPickup(), 100));
        w.start(normal);
        w.clock.advanceTo(normal.getExpectedDropAt());
        w.rides.completeTrip(normal);
        check("Normal drop: captain location == drop == customer location",
                cab.getLocation().equals(ChennaiPlaces.ADYAR) && c.getLocation().equals(ChennaiPlaces.ADYAR));
        check("Captain's free-from time is the drop time", cab.getAvailableFrom().equals(normal.getDroppedAt()));

        Ride early = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.TAMBARAM, VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
        w.arrive(early);
        w.start(early);
        w.clock.advanceMinutes(10);
        w.rides.endTripEarly(early);
        Location stop = early.getDrop();
        check("End early: stop point is between pickup and the booked drop",
                !stop.equals(ChennaiPlaces.TAMBARAM)
                        && ChennaiPlaces.ADYAR.distanceTo(stop) < ChennaiPlaces.ADYAR.distanceTo(ChennaiPlaces.TAMBARAM));
        check("End early: captain and customer are both at the stop point",
                cab.getLocation().equals(stop) && c.getLocation().equals(stop));
        check("End early: fare uses the actual distance/time, lower than the estimate",
                early.getReceipt().getTotal().isLessThan(early.getEstimate().getTotalFare()) && early.actualTripMinutes() == 10);

        Ride changed = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.GUINDY, VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
        w.arrive(changed);
        w.start(changed);
        w.clock.advanceMinutes(3);
        Location changePoint = w.rides.changeDestination(changed, ChennaiPlaces.VELACHERY);
        w.clock.advanceTo(changed.getExpectedDropAt());
        w.rides.completeTrip(changed);
        double expectedKm = stop.distanceTo(changePoint) + changePoint.distanceTo(ChennaiPlaces.VELACHERY);
        check("Destination change: captain and customer end at the NEW drop",
                cab.getLocation().equals(ChennaiPlaces.VELACHERY) && c.getLocation().equals(ChennaiPlaces.VELACHERY));
        check("Destination change: billed distance = pickup -> change point -> new drop",
                Math.abs(changed.actualDistanceKm() - expectedKm) < 0.001);
        check("Destination change is allowed only once", throwsA(InvalidRideStatusException.class,
                () -> changed.changeDestination(ChennaiPlaces.ADYAR, w.clock.now(), 5)));
        check("A customer cannot travel on their own during a ride", throwsA(IllegalStateException.class, () -> {
            Ride busy = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.GUINDY, VehicleType.CAB_ECONOMY, 1, PaymentMethod.UPI);
            c.travelOnOwnTo(ChennaiPlaces.EGMORE);
        }));
    }

    // ====================================================================== 5. matching

    private static void checkMatching() throws Exception {
        World w = new World(12, 0);
        Location pickup = ChennaiPlaces.T_NAGAR;
        Captain near = w.captain("Near Bike", ChennaiPlaces.T_NAGAR, new Bike(w.plate(), "Honda Activa"));
        Captain far = w.captain("Far Bike", ChennaiPlaces.TAMBARAM, new Bike(w.plate(), "Honda Activa"));
        Captain auto = w.captain("Auto Here", ChennaiPlaces.T_NAGAR, new Auto(w.plate(), "TVS King"));
        Captain offline = w.captain("Offline Bike", ChennaiPlaces.T_NAGAR, new Bike(w.plate(), "TVS Jupiter"));
        w.captains.goOffline(offline);
        Captain kyc = w.captains.register(new Captain("Kyc Bike", w.phone(), ChennaiPlaces.T_NAGAR, new Bike(w.plate(), "Honda Activa")));
        LocalDateTime now = w.clock.now();

        List<Captain> eligible = w.matching.findEligibleCaptains(pickup, VehicleType.BIKE, PaymentMethod.UPI, null, now);
        check("Only the near, online, verified bike captain is eligible", eligible.size() == 1 && eligible.get(0).equals(near));
        check("Captain outside the radius is never matched", !eligible.contains(far)
                && w.matching.ineligibilityReason(far, pickup, VehicleType.BIKE, PaymentMethod.UPI, null, now).startsWith("too far"));
        check("Offline captain is never matched", !eligible.contains(offline));
        check("KYC-pending captain is never matched and cannot go online", !eligible.contains(kyc)
                && throwsA(CaptainNotEligibleException.class, () -> w.captains.goOnline(kyc, ChennaiPlaces.T_NAGAR)));
        check("Wrong vehicle type is never matched (auto for a bike request)", !eligible.contains(auto));

        Customer c = w.customer("Matcher", pickup, Money.of(500));
        Ride ride = w.rides.bookRide(c, pickup, ChennaiPlaces.GUINDY, VehicleType.BIKE, 1, PaymentMethod.UPI);
        check("Nearest eligible captain gets the ride", ride.getCaptain().equals(near));
        Customer other = w.customer("Other", pickup, Money.of(500));
        check("Busy captain is never matched (the only bike is on a ride)",
                throwsA(NoCaptainAvailableException.class,
                        () -> w.rides.bookRide(other, pickup, ChennaiPlaces.ADYAR, VehicleType.BIKE, 1, PaymentMethod.UPI)));
        check("A failed search is recorded as CANCELLED (NO_CAPTAIN_AVAILABLE) in history",
                other.getRideHistory().get(0).getCancellationReason() == CancellationReason.NO_CAPTAIN_AVAILABLE);

        // Reject: nearest rejects, next-nearest accepts; 3 rejections -> no captain
        World r = new World(10, 0);
        Customer lakshmi = r.customer("Rejectee", ChennaiPlaces.MYLAPORE, Money.of(500));
        Captain picky = r.captain(new Captain("Picky", r.phone(), ChennaiPlaces.ADYAR, new Bike(r.plate(), "Honda Activa"), 5.0));
        Captain willing = r.captain("Willing", ChennaiPlaces.ASHOK_NAGAR, new Bike(r.plate(), "TVS Jupiter"));
        Ride rejected = r.rides.bookRide(lakshmi, lakshmi.getLocation(), ChennaiPlaces.VADAPALANI, VehicleType.BIKE, 1,
                PaymentMethod.UPI);
        check("Nearest captain rejects, next-nearest accepts",
                rejected.getCaptain().equals(willing) && rejected.getOfferLog().size() == 2
                        && rejected.getOfferLog().get(0).contains("Picky") && rejected.getOfferLog().get(0).contains("REJECTED"));
        check("A captain who rejected THIS ride is never re-offered it",
                "already rejected/cancelled this ride".equals(r.matching.ineligibilityReason(picky, lakshmi.getLocation(),
                        VehicleType.BIKE, PaymentMethod.UPI, rejected, r.clock.now())));
        check("Acceptance rate is tracked (Picky 0%, Willing 100%)",
                picky.getAcceptanceRate() == 0.0 && willing.getAcceptanceRate() == 100.0);

        World three = new World(10, 0);
        Customer c3 = three.customer("Unlucky", ChennaiPlaces.MYLAPORE, Money.of(500));
        Location[] homes = {ChennaiPlaces.MYLAPORE, ChennaiPlaces.ROYAPETTAH, ChennaiPlaces.ADYAR, ChennaiPlaces.T_NAGAR};
        for (Location home : homes) {
            three.captain(new Captain("Short " + home.getName(), three.phone(), home, new Bike(three.plate(), "Honda Activa"), 2.0));
        }
        Ride noOne = null;
        boolean threw = false;
        try {
            three.rides.bookRide(c3, c3.getLocation(), ChennaiPlaces.VADAPALANI, VehicleType.BIKE, 1, PaymentMethod.UPI);
        } catch (NoCaptainAvailableException e) {
            threw = true;
            noOne = c3.getRideHistory().get(0);
        }
        check("After 3 rejections: 'No captains available' (4th captain never offered)",
                threw && noOne.getOfferLog().size() == 3 && noOne.getStatus() == RideStatus.CANCELLED);
    }

    // ====================================================================== 6. canonical

    private static void checkCanonicalCase() throws Exception {
        World w = new World(9, 10);
        Captain senthil = w.captain("Senthil", ChennaiPlaces.EGMORE, new CabEconomy(w.plate(), "Maruti Dzire"));
        Captain rajesh = w.captain("Rajesh", ChennaiPlaces.ROYAPETTAH, new CabEconomy(w.plate(), "Hyundai Aura"));
        Customer arun = w.customer("Arun", ChennaiPlaces.EGMORE, Money.of(2000));
        Customer divya = w.customer("Divya", ChennaiPlaces.EGMORE, Money.of(2000));
        Customer harish = w.customer("Harish", ChennaiPlaces.CHROMEPET, Money.of(2000));

        Ride canonical = w.rides.bookRide(arun, arun.getLocation(), ChennaiPlaces.TAMBARAM, VehicleType.CAB_ECONOMY, 1,
                PaymentMethod.WALLET);
        check("Egmore -> Tambaram goes to Senthil at Egmore", canonical.getCaptain().equals(senthil));
        w.completeNormally(canonical);
        check("After the drop, Senthil is at Tambaram", senthil.getLocation().equals(ChennaiPlaces.TAMBARAM));

        Ride fromEgmore = w.rides.bookRide(divya, divya.getLocation(), ChennaiPlaces.ANNA_NAGAR, VehicleType.CAB_ECONOMY, 1,
                PaymentMethod.UPI);
        check("A new Egmore request NEVER goes to Senthil (now in Tambaram)",
                !fromEgmore.getCaptain().equals(senthil) && fromEgmore.getCaptain().equals(rajesh));
        Ride fromChromepet = w.rides.bookRide(harish, harish.getLocation(), ChennaiPlaces.MEDAVAKKAM, VehicleType.CAB_ECONOMY, 1,
                PaymentMethod.UPI);
        check("A Chromepet request (within radius) goes to Senthil at Tambaram", fromChromepet.getCaptain().equals(senthil));
        check("Pallavaram is also within Senthil's radius from Tambaram",
                ChennaiPlaces.TAMBARAM.distanceTo(ChennaiPlaces.PALLAVARAM) <= MatchingService.MATCH_RADIUS_KM
                        && ChennaiPlaces.TAMBARAM.distanceTo(ChennaiPlaces.EGMORE) > MatchingService.MATCH_RADIUS_KM);
    }

    // ====================================================================== 7. cancellations

    private static void checkCancellations() throws Exception {
        World w = new World(11, 0);
        Customer c = w.customer("Canceller", ChennaiPlaces.ANNA_NAGAR, Money.of(1000));
        Captain bike = w.captain("Cancel Bike", ChennaiPlaces.KILPAUK, new Bike(w.plate(), "TVS Jupiter"));

        Ride grace = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KOYAMBEDU, VehicleType.BIKE, 1, PaymentMethod.UPI);
        w.clock.advanceMinutes(2);
        check("Cancel exactly 2 minutes after assignment is free (grace period)",
                w.rides.cancelByCustomer(grace).isZero() && c.getOutstandingFeeTotal().isZero());

        Ride late = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KOYAMBEDU, VehicleType.BIKE, 1, PaymentMethod.UPI);
        w.clock.advanceMinutes(3);
        check("Cancel 3 minutes after assignment costs ₹20 for a bike",
                w.rides.cancelByCustomer(late).equals(Money.of(20)) && c.getOutstandingFeeTotal().equals(Money.of(20)));

        Captain auto = w.captain("Cancel Auto", ChennaiPlaces.ANNA_NAGAR, new Auto(w.plate(), "Bajaj RE Auto"));
        Ride arrived = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KOYAMBEDU, VehicleType.AUTO, 1, PaymentMethod.UPI);
        w.arrive(arrived);
        check("Cancel after the captain arrived costs ₹30 for an auto (even inside 2 minutes)",
                w.rides.cancelByCustomer(arrived).equals(Money.of(30)) && c.getOutstandingFeeTotal().equals(Money.of(50)));

        Ride next = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KILPAUK, VehicleType.AUTO, 1, PaymentMethod.UPI);
        FareReceipt nextReceipt = w.completeNormally(next);
        check("Fees appear on the NEXT ride's receipt", nextReceipt.getPreviousFeesTotal().equals(Money.of(50))
                && nextReceipt.getPreviousFees().size() == 2);
        Ride afterThat = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ANNA_NAGAR, VehicleType.AUTO, 1, PaymentMethod.UPI);
        FareReceipt afterReceipt = w.completeNormally(afterThat);
        check("...exactly once (the ride after that has no fee)",
                afterReceipt.getPreviousFeesTotal().isZero() && c.getOutstandingFeeTotal().isZero());
        EarningsService.DayEarnings bikeDay = w.earnings.getDayEarnings(bike, DAY);
        check("The ₹20 fee goes to the captain who was let down (₹16 after commission)",
                bikeDay.getNetEarnings().equals(Money.of(16)) && bikeDay.getRides() == 0);

        // No-show
        World n = new World(13, 0);
        Customer ghost = n.customer("Ghost", ChennaiPlaces.GUINDY, Money.of(500));
        Captain waiter = n.captain("Waiter", ChennaiPlaces.GUINDY, new Bike(n.plate(), "Honda Activa"));
        Ride noShow = n.rides.bookRide(ghost, ghost.getLocation(), ChennaiPlaces.ADYAR, VehicleType.BIKE, 1, PaymentMethod.UPI);
        n.arrive(noShow);
        n.clock.advanceMinutes(4);
        check("No-show cannot be declared before 5 minutes of waiting",
                throwsA(InvalidRideStatusException.class, () -> n.rides.cancelForNoShow(noShow)));
        n.clock.advanceMinutes(1);
        Money noShowFee = n.rides.cancelForNoShow(noShow);
        check("No-show after 5 minutes: fee charged, captain free at the pickup",
                noShowFee.equals(VehicleType.BIKE.getNoShowFee()) && noShow.getCancellationReason() == CancellationReason.CUSTOMER_NO_SHOW
                        && waiter.getStatus() == CaptainStatus.AVAILABLE && waiter.getLocation().equals(ChennaiPlaces.GUINDY)
                        && ghost.getOutstandingFeeTotal().equals(noShowFee));

        // Waiting charge
        World wt = new World(12, 30);
        Customer slow = wt.customer("Slow", ChennaiPlaces.ANNA_NAGAR, Money.of(500));
        wt.captain("Patient", ChennaiPlaces.ANNA_NAGAR, new Auto(wt.plate(), "Bajaj RE Auto"));
        Ride waiting = wt.rides.bookRide(slow, slow.getLocation(), ChennaiPlaces.KOYAMBEDU, VehicleType.AUTO, 1, PaymentMethod.UPI);
        wt.arrive(waiting);
        wt.clock.advanceMinutes(4);
        wt.start(waiting);
        wt.clock.advanceTo(waiting.getExpectedDropAt());
        FareReceipt waitReceipt = wt.rides.completeTrip(waiting);
        check("4 minutes of waiting = 3 free + ₹1 charged", waitReceipt.getWaitingCharge().equals(Money.of(1)));
    }

    // ====================================================================== 8. booking

    private static void checkBookingRules() throws Exception {
        World w = new World(10, 0);
        Customer c = w.customer("Booker", ChennaiPlaces.GUINDY, Money.of(500));
        w.captain("Book Bike", ChennaiPlaces.GUINDY, new Bike(w.plate(), "Honda Activa"));
        w.captain("Book Auto", ChennaiPlaces.GUINDY, new Auto(w.plate(), "TVS King"));

        check("Pickup equal to drop is rejected", throwsA(InvalidBookingException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.GUINDY, VehicleType.BIKE, 1)));
        check("Drop under 500 m away is rejected", throwsA(InvalidBookingException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, new Location("Next street", 13.0100, 80.2206), VehicleType.BIKE, 1)));
        check("Drop outside the service area (Mahabalipuram) is rejected", throwsA(OutOfServiceAreaException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.MAHABALIPURAM, VehicleType.BIKE, 1)));
        check("A pickup outside the service area is rejected", throwsA(OutOfServiceAreaException.class,
                () -> w.rides.getFareEstimates(new Location("Sriperumbudur", 12.9675, 79.9419), ChennaiPlaces.GUINDY)));
        // The Chennai box is only ~53 km corner to corner, so the 60 km rule is checked in a wider test region.
        World wide = new World(at(10, 0), new ServiceArea("Greater Chennai test region", 12.40, 13.40, 79.60, 80.40));
        Location ponneri = new Location("Ponneri", 13.3380, 80.1944);
        Location chengalpattu = new Location("Chengalpattu", 12.6921, 79.9766);
        check("A trip over 60 km is rejected (Ponneri -> Chengalpattu, "
                        + String.format("%.1f", ponneri.distanceTo(chengalpattu)) + " km)",
                throwsA(InvalidBookingException.class, () -> wide.rides.getFareEstimates(ponneri, chengalpattu))
                        && !throwsA(InvalidBookingException.class, () -> wide.rides.getFareEstimates(ChennaiPlaces.EGMORE,
                        ChennaiPlaces.TAMBARAM)));
        check("Too many passengers for the vehicle (2 on a bike) is rejected", throwsA(InvalidBookingException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.ADYAR, VehicleType.BIKE, 2)));
        check("Pickup must be where the customer is (no teleporting customers)", throwsA(InvalidBookingException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.EGMORE, ChennaiPlaces.ADYAR, VehicleType.BIKE, 1)));
        check("Unknown coupon is rejected", throwsA(InvalidBookingException.class,
                () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.ADYAR, VehicleType.BIKE, 1, PaymentMethod.UPI, "FREE100")));

        Ride active = w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.ADYAR, VehicleType.BIKE, 1);
        check("Overloaded bookRide without a payment method defaults to Cash",
                active.getRequest().getPaymentMethod() == PaymentMethod.CASH);
        check("One active ride per customer: second booking throws ActiveRideExistsException",
                throwsA(ActiveRideExistsException.class,
                        () -> w.rides.bookRide(c, ChennaiPlaces.GUINDY, ChennaiPlaces.T_NAGAR, VehicleType.AUTO, 1)));
        w.completeNormally(active);

        World p = new World(19, 0);
        Customer h = p.customer("Harish", ChennaiPlaces.MEDAVAKKAM, Money.of(60), "h@okhdfcbank");
        p.payments.reportUpiOutage("okhdfcbank", at(18, 45), at(19, 45));
        p.captain("Pending Cab", ChennaiPlaces.MEDAVAKKAM, new CabEconomy(p.plate(), "Maruti Dzire"));
        p.captain("Home Bike", ChennaiPlaces.VELACHERY, new Bike(p.plate(), "Honda Activa"));
        Ride pending = p.rides.bookRide(h, h.getLocation(), ChennaiPlaces.VELACHERY, VehicleType.CAB_ECONOMY, 1, PaymentMethod.WALLET);
        p.completeNormally(pending);
        check("Payment pending blocks a new booking (PaymentPendingException)",
                pending.getStatus() == RideStatus.PAYMENT_PENDING && throwsA(PaymentPendingException.class,
                        () -> p.rides.bookRide(h, h.getLocation(), ChennaiPlaces.GUINDY, VehicleType.BIKE, 1)));
        check("UPI retry during the bank outage fails and stays PAYMENT_PENDING",
                p.rides.retryPayment(pending, PaymentMethod.UPI) == PaymentStatus.FAILED
                        && pending.getStatus() == RideStatus.PAYMENT_PENDING);
        p.rides.retryPayment(pending, PaymentMethod.CASH);
        Ride allowed = p.rides.bookRide(h, h.getLocation(), ChennaiPlaces.GUINDY, VehicleType.BIKE, 1);
        check("After paying cash, booking succeeds", allowed.getStatus() == RideStatus.CAPTAIN_ASSIGNED);

        List<Ride> history = h.getRideHistory();
        boolean ordered = true;
        for (int i = 1; i < history.size(); i++) {
            if (history.get(i).getRequestedAt().isBefore(history.get(i - 1).getRequestedAt())) {
                ordered = false;
            }
        }
        check("Ride history is chronological", ordered && history.size() == 2);
    }

    // ====================================================================== 9. payments

    private static void checkPayments() throws Exception {
        World w = new World(19, 0);
        Customer c = w.customer("Low Wallet", ChennaiPlaces.MEDAVAKKAM, Money.of(60));
        w.captain("Pay Cab", ChennaiPlaces.MEDAVAKKAM, new CabEconomy(w.plate(), "Maruti Dzire"));
        Ride ride = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.VELACHERY, VehicleType.CAB_ECONOMY, 1, PaymentMethod.WALLET);
        w.completeNormally(ride);
        check("Wallet with too little money: ride PAYMENT_PENDING, balance untouched (no partial debit)",
                ride.getStatus() == RideStatus.PAYMENT_PENDING && c.getWalletBalance().equals(Money.of(60)));
        check("Wallet retry throws InsufficientWalletBalanceException, still no partial debit",
                throwsA(InsufficientWalletBalanceException.class, () -> w.rides.retryPayment(ride, PaymentMethod.WALLET))
                        && c.getWalletBalance().equals(Money.of(60)));
        w.customers.topUpWallet(c, Money.of(1000));
        w.rides.retryPayment(ride, PaymentMethod.WALLET);
        Money afterPay = c.getWalletBalance();
        check("After a top-up the wallet pays the exact total",
                ride.getStatus() == RideStatus.COMPLETED && afterPay.equals(Money.of(1060).minus(ride.getReceipt().getTotal())));
        check("No double payment: collecting again throws, balance unchanged",
                throwsA(InvalidRideStatusException.class, () -> w.payments.collect(ride, PaymentMethod.WALLET, w.clock.now()))
                        && c.getWalletBalance().equals(afterPay));
        check("Wallet top-up must be positive and at most ₹10,000",
                throwsA(IllegalArgumentException.class, () -> c.topUpWallet(Money.ZERO))
                        && throwsA(IllegalArgumentException.class, () -> c.topUpWallet(Money.of(10001))));
        check("Money can never be negative", throwsA(IllegalArgumentException.class, () -> Money.of(5).minus(Money.of(6))));
    }

    // ====================================================================== 10. cash dues

    private static void checkCashDues() throws Exception {
        World w = new World(20, 0);
        Customer c = w.customer("Cash Payer", ChennaiPlaces.EGMORE, Money.ZERO);
        Captain murugan = w.captain(new Captain("Murugan", w.phone(), ChennaiPlaces.EGMORE,
                new Auto(w.plate(), "Bajaj RE Auto"), 60, 0, 0, Money.of(490)));
        Captain backup = w.captain("Backup Auto", ChennaiPlaces.KOYAMBEDU, new Auto(w.plate(), "TVS King"));
        check("₹490 owed: not blocked yet", !murugan.isBlockedForCash());
        Ride cash = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ANNA_NAGAR, VehicleType.AUTO, 1, PaymentMethod.CASH);
        w.completeNormally(cash);
        check("Cash ride adds dues (commission + GST) and crosses ₹500 -> blocked",
                murugan.getDuesOwed().equals(Money.of(490).plus(cash.getReceipt().getTotal()
                        .minus(cash.getReceipt().getRideAmount().minus(cash.getReceipt().getRideAmount().percent(20)))))
                        && murugan.isBlockedForCash());
        LocalDateTime now = w.clock.now();
        check("Blocked captain is not eligible for CASH but still eligible for UPI",
                w.matching.ineligibilityReason(murugan, c.getLocation(), VehicleType.AUTO, PaymentMethod.CASH, null, now) != null
                        && w.matching.ineligibilityReason(murugan, c.getLocation(), VehicleType.AUTO, PaymentMethod.UPI, null, now) == null);
        Ride skipped = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.KILPAUK, VehicleType.AUTO, 1, PaymentMethod.CASH);
        check("A cash booking skips the blocked captain even though he is nearest", skipped.getCaptain().equals(backup));
        Money settled = w.captains.settleDues(murugan);
        check("settleDues() pays everything and unblocks the captain",
                settled.isGreaterThan(Captain.CASH_DUES_LIMIT) && murugan.getDuesOwed().isZero() && !murugan.isBlockedForCash()
                        && w.matching.ineligibilityReason(murugan, c.getLocation(), VehicleType.AUTO, PaymentMethod.CASH, null,
                        w.clock.now()) == null);
    }

    // ====================================================================== 11. clock

    private static void checkClock() throws Exception {
        SimulatedClock clock = new SimulatedClock(at(9, 40));
        check("Clock refuses to go backward (advanceTo)", throwsA(IllegalArgumentException.class, () -> clock.advanceTo(at(9, 39))));
        check("Clock refuses negative minutes", throwsA(IllegalArgumentException.class, () -> clock.advanceMinutes(-1)));
        clock.advanceToAtLeast(at(9, 0));
        check("advanceToAtLeast never moves the clock back", clock.now().equals(at(9, 40)));

        World w = new World(9, 0);
        Customer c = w.customer("Timer", ChennaiPlaces.GUINDY, Money.of(500));
        Captain bike = w.captain("Timer Bike", ChennaiPlaces.GUINDY, new Bike(w.plate(), "Honda Activa"));
        Ride first = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.ADYAR, VehicleType.BIKE, 1, PaymentMethod.UPI);
        w.completeNormally(first);
        LocalDateTime finishedAt = first.getDroppedAt();
        check("A captain is not available before the time their last ride ended",
                !bike.isAvailableAt(finishedAt.minusMinutes(1)) && bike.isAvailableAt(finishedAt));
        check("...and cannot accept a ride 'in the past'",
                throwsA(CaptainBusyException.class, () -> bike.acceptRide("RIDE-PAST", finishedAt.minusMinutes(1))));
    }

    // ====================================================================== 12. captains & ratings

    private static void checkCaptainsAndRatings() throws Exception {
        World w = new World(12, 0);
        Customer c = w.customer("Rater", ChennaiPlaces.T_NAGAR, Money.of(2000));
        Captain ramesh = w.captain(new Captain("Ramesh", w.phone(), ChennaiPlaces.T_NAGAR,
                new Auto(w.plate(), "Bajaj RE Auto"), 60, 4, 15, Money.ZERO));
        check("A new captain starts KYC_PENDING",
                new Captain("Newbie", w.phone(), ChennaiPlaces.T_NAGAR, new Bike(w.plate(), "Honda Activa")).getStatus()
                        == CaptainStatus.KYC_PENDING);
        check("4 ratings averaging 3.75 are not flagged yet (needs 5)", !ramesh.isFlagged());
        Ride ride = w.rides.bookRide(c, c.getLocation(), ChennaiPlaces.GUINDY, VehicleType.AUTO, 1, PaymentMethod.UPI);
        w.arrive(ride);
        w.start(ride);
        check("Captain cannot go offline during an active ride",
                throwsA(CaptainBusyException.class, () -> w.captains.goOffline(ramesh)));
        w.clock.advanceTo(ride.getExpectedDropAt());
        w.rides.completeTrip(ride);
        w.captains.goOffline(ramesh);
        check("...but can after the ride ends", ramesh.getStatus() == CaptainStatus.OFFLINE);
        check("Rating must be 1 to 5", throwsA(IllegalArgumentException.class, () -> w.ratings.rateCaptain(ride, 6)));
        w.ratings.rateCaptain(ride, 3);
        check("5th rating of 3 -> average 3.6 < 4.0 -> flagged",
                ramesh.getRatingCount() == 5 && ramesh.isFlagged() && w.ratings.flaggedCaptains().contains(ramesh));
        check("A completed ride can be rated only once per side",
                throwsA(InvalidRideStatusException.class, () -> w.ratings.rateCaptain(ride, 5)));
        w.ratings.rateCustomer(ride, 5);
        check("The captain can rate the customer once too",
                c.getRatingCount() == 1 && throwsA(InvalidRideStatusException.class, () -> w.ratings.rateCustomer(ride, 4)));
        check("Offline captain is never matched", w.matching.ineligibilityReason(ramesh, c.getLocation(), VehicleType.AUTO,
                PaymentMethod.UPI, null, w.clock.now()).equals("offline"));

        check("Money equals by value (₹20.00 == ₹20)", Money.of("20.00").equals(Money.of(20))
                && Money.of("20.00").hashCode() == Money.of(20).hashCode());
        check("Location equals by coordinates, not name",
                new Location("A", 13.0, 80.2).equals(new Location("B", 13.0, 80.2)));
        check("Captains equal by id only", !ramesh.equals(new Captain("Ramesh", ramesh.getPhone(), ChennaiPlaces.T_NAGAR,
                new Auto(w.plate(), "Bajaj RE Auto"))) && ramesh.equals(ramesh));
        check("Vehicles equal by registration number", new Bike("TN-09-AB-4521", "Honda Activa")
                .equals(new Bike("TN-09-AB-4521", "TVS Jupiter")));
        check("Invalid TN plate and phone are rejected", throwsA(IllegalArgumentException.class,
                () -> new Bike("KA-01-AB-1234", "Honda Activa")) && throwsA(IllegalArgumentException.class,
                () -> new Customer("X", "98401 22334", ChennaiPlaces.T_NAGAR, "x@ybl")));
        check("Mahabalipuram is outside and Guduvanchery inside the service area",
                !ServiceArea.chennaiMetro().contains(ChennaiPlaces.MAHABALIPURAM)
                        && ServiceArea.chennaiMetro().contains(ChennaiPlaces.GUDUVANCHERY));
    }

    // ====================================================================== 13. full day

    private static void checkFullDay() throws Exception {
        PrintStream console = System.out;
        ChennaiRideApp app = new ChennaiRideApp();
        try {
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            app.runDay();
        } finally {
            System.setOut(console);
        }
        EarningsService earnings = app.getEarningsService();
        List<Ride> rides = app.getRideService().getAllRides();

        check("Demo day ran: " + rides.size() + " rides, " + earnings.getPaidRides() + " paid",
                rides.size() >= 20 && earnings.getPaidRides() >= 15);

        Money receipts = Money.ZERO;
        int completed = 0;
        boolean anyPending = false;
        for (Ride ride : rides) {
            if (ride.getStatus() == RideStatus.COMPLETED) {
                receipts = receipts.plus(ride.getReceipt().getTotal());
                completed++;
            }
            if (ride.getStatus() == RideStatus.PAYMENT_PENDING || ride.getStatus().isActive()) {
                anyPending = true;
            }
        }
        check("Every ride ends the day COMPLETED or CANCELLED", !anyPending);
        check("Sum of completed receipts == total paid by customers (" + receipts + ")",
                receipts.equals(earnings.getTotalPaidByCustomers()) && completed == earnings.getPaidRides());
        check("Books balance to the paisa: paid = captain earnings + commission + GST",
                earnings.getTotalPaidByCustomers().equals(earnings.getTotalCaptainEarnings()
                        .plus(earnings.getTotalCommission()).plus(earnings.getTotalGst())) && earnings.isBalanced());
        check("Cash + digital collections == total paid",
                earnings.getTotalCash().plus(earnings.getTotalDigital()).equals(earnings.getTotalPaidByCustomers()));

        Money perCaptain = Money.ZERO;
        for (EarningsService.DayEarnings day : earnings.getAllDayEarnings()) {
            perCaptain = perCaptain.plus(day.getNetEarnings());
        }
        check("Per-captain net earnings add up to the total captain earnings",
                perCaptain.equals(earnings.getTotalCaptainEarnings()));

        boolean timelinesOrdered = true;
        for (Ride ride : rides) {
            if (!timelineOrdered(ride)) {
                timelinesOrdered = false;
            }
        }
        check("Every ride's timeline only moves forward in time", timelinesOrdered);

        boolean neverOverlaps = true;
        boolean neverDoubleBooked = true;
        for (Captain captain : app.getCaptainService().getAllCaptains()) {
            List<Ride> trips = new ArrayList<>();
            for (Ride ride : app.getRideService().getRidesOf(captain)) {
                if (ride.getStartedAt() != null) {
                    trips.add(ride);
                }
            }
            for (int i = 1; i < trips.size(); i++) {
                Ride previous = trips.get(i - 1);
                Ride next = trips.get(i);
                if (next.getStartedAt().isBefore(previous.getDroppedAt())) {
                    neverOverlaps = false;
                }
                if (next.getAssignedAt().isBefore(previous.getDroppedAt())) {
                    neverDoubleBooked = false;
                }
            }
        }
        check("A captain's next ride never starts before the previous one ended", neverOverlaps);
        check("A captain is never assigned a new ride before dropping the previous customer", neverDoubleBooked);

        boolean locationsMatch = true;
        for (Captain captain : app.getCaptainService().getAllCaptains()) {
            List<Ride> trips = app.getRideService().getRidesOf(captain);
            Ride last = null;
            for (Ride ride : trips) {
                if (ride.getDroppedAt() != null) {
                    last = ride;
                }
            }
            boolean cancelledLater = false;
            for (Ride ride : trips) {
                if (last != null && ride.getStatus() == RideStatus.CANCELLED && ride.getRequestedAt().isAfter(last.getRequestedAt())) {
                    cancelledLater = true;
                }
            }
            if (last != null && !cancelledLater && !captain.getLocation().equals(last.getDrop())) {
                locationsMatch = false;
            }
        }
        check("At end of day every captain is at their last drop point", locationsMatch);

        Customer priya = app.getCustomerService().getAllCustomers().get(0);
        check("Priya's history keeps cancelled rides, in order", priya.getRideHistory().size() == 5
                && priya.getRideHistory().get(1).getStatus() == RideStatus.CANCELLED);
    }

    // ====================================================================== helpers

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    private static boolean throwsA(Class<? extends Exception> expected, Action action) {
        try {
            action.run();
            return false;
        } catch (Exception e) {
            return expected.isInstance(e);
        }
    }

    private static boolean timelineOrdered(Ride ride) {
        List<TimelineEntry> entries = ride.getTimeline();
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).getAt().isBefore(entries.get(i - 1).getAt())) {
                return false;
            }
        }
        return true;
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(DAY, LocalTime.of(hour, minute));
    }

    /** A small, fresh copy of the whole app (no notifiers, so it prints nothing). */
    private static final class World {
        private static int counter = 1000;

        final SimulatedClock clock;
        final CustomerService customers = new CustomerService();
        final CaptainService captains;
        final FareService fares;
        final MatchingService matching;
        final EarningsService earnings = new EarningsService();
        final PaymentService payments = new PaymentService(earnings);
        final RatingService ratings;
        final RideService rides;

        World(int hour, int minute) {
            this(at(hour, minute));
        }

        World(LocalDateTime start) {
            this(start, ServiceArea.chennaiMetro());
        }

        World(LocalDateTime start, ServiceArea area) {
            clock = new SimulatedClock(start);
            fares = new FareService(area);
            captains = new CaptainService(clock);
            matching = new MatchingService(captains, fares);
            ratings = new RatingService(captains);
            rides = new RideService(clock, matching, fares, payments, new ArrayList<>());
        }

        String plate() {
            counter++;
            return "TN-01-SC-" + counter;
        }

        String phone() {
            counter++;
            return "+91 90000 " + String.format("%05d", counter);
        }

        Customer customer(String name, Location at, Money wallet) {
            return customer(name, at, wallet, name.toLowerCase().replace(' ', '.') + "@okaxis");
        }

        Customer customer(String name, Location at, Money wallet, String upi) {
            return customers.register(new Customer(name, phone(), at, upi, wallet));
        }

        Captain captain(String name, Location at, Vehicle vehicle) throws CaptainNotEligibleException {
            return captain(new Captain(name, phone(), at, vehicle));
        }

        Captain captain(Captain captain) throws CaptainNotEligibleException {
            captains.register(captain);
            captains.verifyKyc(captain);
            captains.goOnline(captain, captain.getLocation());
            return captain;
        }

        void arrive(Ride ride) throws InvalidRideStatusException {
            clock.advanceToAtLeast(ride.getExpectedArrivalAt());
            rides.captainArrived(ride);
        }

        void start(Ride ride) throws Exception {
            rides.startRide(ride, ride.revealOtpTo(ride.getCustomer()));
        }

        FareReceipt completeNormally(Ride ride) throws Exception {
            arrive(ride);
            start(ride);
            clock.advanceTo(ride.getExpectedDropAt());
            return rides.completeTrip(ride);
        }
    }
}
