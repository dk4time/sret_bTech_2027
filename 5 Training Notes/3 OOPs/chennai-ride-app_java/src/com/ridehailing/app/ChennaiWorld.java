package com.ridehailing.app;

import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.common.ServiceArea;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.Auto;
import com.ridehailing.model.vehicle.Bike;
import com.ridehailing.model.vehicle.CabEconomy;
import com.ridehailing.model.vehicle.CabPremium;
import com.ridehailing.model.vehicle.Vehicle;
import com.ridehailing.notification.Notifier;
import com.ridehailing.service.CaptainService;
import com.ridehailing.service.CustomerService;
import com.ridehailing.service.EarningsService;
import com.ridehailing.service.FareService;
import com.ridehailing.service.MatchingService;
import com.ridehailing.service.PaymentService;
import com.ridehailing.service.RatingService;
import com.ridehailing.service.RideAutopilot;
import com.ridehailing.service.RideService;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * The whole app wired together and seeded with the same Chennai people every time:
 * 5 customers, 12 captains (Mani's KYC still pending) and the evening HDFC UPI outage.
 *
 * Both the scripted demo and the interactive mode build one of these, so the SAME objects
 * power both. COMPOSITION: the world creates and owns its services.
 */
public final class ChennaiWorld {

    public static final LocalDate DAY = LocalDate.of(2026, 9, 14);  // a Monday

    private final SimulatedClock clock;
    private final ServiceArea serviceArea = ServiceArea.chennaiMetro();
    private final CustomerService customerService = new CustomerService();
    private final CaptainService captainService;
    private final FareService fareService;
    private final MatchingService matchingService;
    private final EarningsService earningsService = new EarningsService();
    private final PaymentService paymentService;
    private final RatingService ratingService;
    private final RideService rideService;
    private final RideAutopilot autopilot;

    public ChennaiWorld(SimulatedClock clock, List<Notifier> notifiers) {
        this.clock = clock;
        this.captainService = new CaptainService(clock);
        this.fareService = new FareService(serviceArea);
        this.matchingService = new MatchingService(captainService, fareService);
        this.paymentService = new PaymentService(earningsService);
        this.ratingService = new RatingService(captainService);
        this.rideService = new RideService(clock, matchingService, fareService, paymentService, notifiers);
        this.autopilot = new RideAutopilot(clock, rideService);
        seed();
    }

    public static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(DAY, LocalTime.of(hour, minute));
    }

    private void seed() {
        // Customers: name, phone, where they are this morning, UPI id, opening wallet balance.
        customerService.register(new Customer("Priya", "+91 98401 22334", ChennaiPlaces.VELACHERY,
                "priya.r@okaxis", Money.of(2000)));
        customerService.register(new Customer("Divya", "+91 94440 51872", ChennaiPlaces.CHENNAI_CENTRAL,
                "divya.k@oksbi", Money.of(300)));
        customerService.register(new Customer("Arun", "+91 99620 17745", ChennaiPlaces.EGMORE,
                "arun.v@okicici", Money.of(1500)));
        customerService.register(new Customer("Lakshmi", "+91 90030 66218", ChennaiPlaces.MYLAPORE,
                "lakshmi.s@ybl", Money.of(800)));
        customerService.register(new Customer("Harish", "+91 97899 40561", ChennaiPlaces.CHROMEPET,
                "harish.m@okhdfcbank", Money.of(60)));

        // Captains. UPCASTING: each vehicle is held in a plain Vehicle variable.
        Vehicle muruganAuto = new Auto("TN-09-AB-4521", "Bajaj RE Auto");
        captainService.register(new Captain("Murugan", "+91 98410 45210", ChennaiPlaces.CHENNAI_CENTRAL,
                muruganAuto, 60, 120, 552, Money.of(465)));      // carries ₹465 unpaid dues from last week
        captainService.register(new Captain("Karthik", "+91 98840 78130", ChennaiPlaces.VELACHERY,
                new Bike("TN-22-CK-7813", "Honda Activa")));
        captainService.register(new Captain("Selvi", "+91 96000 11902", ChennaiPlaces.ASHOK_NAGAR,
                new Bike("TN-07-BX-1190", "TVS Jupiter", "Grey")));
        captainService.register(new Captain("Ganesh", "+91 93810 90445", ChennaiPlaces.ADYAR,
                new Bike("TN-11-EZ-9044", "Honda Activa"), 5.0));   // prefers short trips
        captainService.register(new Captain("Suresh", "+91 95000 33817", ChennaiPlaces.ANNA_NAGAR,
                new Bike("TN-18-JK-3381", "TVS Jupiter")));
        captainService.register(new Captain("Anbu", "+91 98401 55306", ChennaiPlaces.MYLAPORE,
                new Auto("TN-12-AF-5530", "TVS King")));
        captainService.register(new Captain("Ramesh", "+91 97100 71024", ChennaiPlaces.T_NAGAR,
                new Auto("TN-14-GH-7102", "Bajaj RE Auto"), 60, 4, 15, Money.ZERO)); // 4 past ratings, avg 3.75
        captainService.register(new Captain("Senthil", "+91 99400 33452", ChennaiPlaces.EGMORE,
                new CabEconomy("TN-02-AR-3345", "Maruti Dzire")));
        captainService.register(new Captain("Rajesh", "+91 98843 66210", ChennaiPlaces.ROYAPETTAH,
                new CabEconomy("TN-05-BQ-6621", "Hyundai Aura")));
        captainService.register(new Captain("Balaji", "+91 94443 88901", ChennaiPlaces.PALLAVARAM,
                new CabPremium("TN-10-CM-8890", "Toyota Innova Crysta")));
        captainService.register(new Captain("Vignesh", "+91 90940 22170", ChennaiPlaces.ANNA_NAGAR,
                new CabPremium("TN-04-DK-2217", "Toyota Innova Crysta")));
        Captain mani = captainService.register(new Captain("Mani", "+91 91500 66503", ChennaiPlaces.TAMBARAM,
                new CabEconomy("TN-19-LM-6650", "Maruti Dzire")));    // documents still under review

        for (Captain captain : captainService.getAllCaptains()) {
            if (captain != mani) {
                captainService.verifyKyc(captain);
            }
        }

        // Deterministic UPI failure: HDFC Bank's UPI servers are down in the evening.
        paymentService.reportUpiOutage("okhdfcbank", at(18, 45), at(19, 45));
    }

    public Customer customer(String name) {
        for (Customer customer : customerService.getAllCustomers()) {
            if (customer.getName().equals(name)) {
                return customer;
            }
        }
        throw new IllegalArgumentException("No customer named " + name);
    }

    public Captain captain(String name) {
        return captainService.findByName(name);
    }

    public SimulatedClock getClock() {
        return clock;
    }

    public ServiceArea getServiceArea() {
        return serviceArea;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public CaptainService getCaptainService() {
        return captainService;
    }

    public FareService getFareService() {
        return fareService;
    }

    public MatchingService getMatchingService() {
        return matchingService;
    }

    public EarningsService getEarningsService() {
        return earningsService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public RatingService getRatingService() {
        return ratingService;
    }

    public RideService getRideService() {
        return rideService;
    }

    public RideAutopilot getAutopilot() {
        return autopilot;
    }
}
