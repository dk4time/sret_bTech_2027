package com.ridehailing;

/*
 * OOP Concepts in Java — explained through a Rapido-style ride app (Chennai)
 * ==========================================================================
 * Compile & run (Java 17+, JDK only):
 *     javac OopsConceptsJava.java
 *     java OopsConceptsJava
 *
 * A menu lets you run one concept at a time (or all of them), so you can
 * teach each concept separately. All classes live in this one file for
 * classroom convenience (only one of them may be public).
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

public class OopsConceptsJava {

    static void header(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60));
    }

    // Menu titles, in the same order as the demos in runDemo()
    private static final String[] MENU = {
            "Class and object",
            "Constructors (overloading + this() chaining)",
            "static vs instance members",
            "Encapsulation",
            "Abstraction (abstract class vs interface)",
            "Inheritance",
            "Polymorphism",
            "Composition vs aggregation",
            "final keyword + immutable class",
            "toString(), equals(), hashCode()",
            "Custom exceptions (checked vs unchecked)"
    };

    public static void main(String[] args) {
        // Print ₹ and ✔/✘ correctly on every console (Windows cmd included)
        System.setOut(new java.io.PrintStream(
                new java.io.FileOutputStream(java.io.FileDescriptor.out), true,
                java.nio.charset.StandardCharsets.UTF_8));

        Scanner sc = new Scanner(System.in);
        while (true) {
            showMenu();
            System.out.print("\nEnter your choice: ");
            if (!sc.hasNextLine()) {                     // input ended (EOF)
                break;
            }
            String choice = sc.nextLine().trim().toLowerCase();

            if (choice.equals("0")) {
                break;
            } else if (choice.equals("a")) {
                for (int i = 1; i <= MENU.length; i++) {
                    runDemo(i);
                }
                pause(sc);
            } else {
                try {
                    int n = Integer.parseInt(choice);
                    if (n < 1 || n > MENU.length) {
                        throw new NumberFormatException();
                    }
                    runDemo(n);
                    pause(sc);
                } catch (NumberFormatException e) {
                    System.out.println("✘ Invalid choice '" + choice + "'. Enter 1-"
                            + MENU.length + ", A or 0.");
                }
            }
        }
        System.out.println("\nThank you! Keep practising OOP.");
    }

    static void showMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("   OOP CONCEPTS IN JAVA  -  Chennai Ride App");
        System.out.println("=".repeat(60));
        for (int i = 0; i < MENU.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, MENU[i]);
        }
        System.out.println("   A. Run all concepts");
        System.out.println("   0. Exit");
    }

    static void pause(Scanner sc) {
        System.out.print("\nPress Enter to return to the menu...");
        if (sc.hasNextLine()) {
            sc.nextLine();
        }
    }

    static void runDemo(int choice) {
        switch (choice) {
            case 1:  demoClassAndObject(); break;
            case 2:  demoConstructors(); break;
            case 3:  demoStaticVsInstance(); break;
            case 4:  demoEncapsulation(); break;
            case 5:  demoAbstraction(); break;
            case 6:  demoInheritance(); break;
            case 7:  demoPolymorphism(); break;
            case 8:  demoCompositionAggregation(); break;
            case 9:  demoFinal(); break;
            case 10: demoObjectMethods(); break;
            case 11: demoExceptions(); break;
            default: System.out.println("✘ Unknown option");
        }
    }

    // -----------------------------------------------------------------------
    // 1. CLASS AND OBJECT
    //    A class is the blueprint; an object is a real thing built from it.
    // -----------------------------------------------------------------------
    static void demoClassAndObject() {
        header("1. CLASS AND OBJECT");
        Customer priya = new Customer("Priya", "+91 98400 12345");     // object 1
        Customer karthik = new Customer("Karthik", "+91 98410 67890"); // object 2, same blueprint
        System.out.println(priya.greet());
        System.out.println(karthik.greet());
        System.out.println("Different objects? " + (priya != karthik));
    }

    // -----------------------------------------------------------------------
    // 2. CONSTRUCTORS — default, parameterised, overloading, this() chaining
    // -----------------------------------------------------------------------
    static void demoConstructors() {
        header("2. CONSTRUCTORS (overloading + this() chaining)");
        RideRequest r1 = new RideRequest("Velachery", "Guindy");                 // 2 args
        RideRequest r2 = new RideRequest("Adyar", "T. Nagar", "Auto");           // 3 args
        RideRequest r3 = new RideRequest("Airport", "Anna Nagar", "CabPremium", 4); // 4 args
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);
    }

    // -----------------------------------------------------------------------
    // 3. STATIC vs INSTANCE MEMBERS
    //    static   -> ONE copy, belongs to the class, shared by all objects
    //    instance -> each object gets its OWN copy
    // -----------------------------------------------------------------------
    static void demoStaticVsInstance() {
        header("3. STATIC vs INSTANCE MEMBERS");
        RideCounter a = new RideCounter("Egmore", "Tambaram");
        RideCounter b = new RideCounter("Velachery", "Guindy");
        System.out.println(a.getRideId() + " " + a.getRoute());
        System.out.println(b.getRideId() + " " + b.getRoute());
        System.out.println("Shared GST rate: " + RideCounter.GST_RATE
                + " | Total rides (static): " + RideCounter.getTotalRides());
    }

    // -----------------------------------------------------------------------
    // 4. ENCAPSULATION
    //    private fields + public methods that validate every change.
    //    No setStatus() -> status changes only through meaningful actions.
    // -----------------------------------------------------------------------
    static void demoEncapsulation() {
        header("4. ENCAPSULATION");
        Ride ride = new Ride("Egmore", "Tambaram", "4821");
        System.out.println("Status: " + ride.getStatus());
        // ride.status = "COMPLETED";   // compile error: status is private
        // ride.otp;                    // compile error: otp is private, no getter
        System.out.println("✘ ride.status / ride.otp -> compile error (private)");

        try {
            ride.start("4821");                         // wrong state
        } catch (IllegalStateException e) {
            System.out.println("✘ " + e.getMessage());
        }
        ride.markArrived();
        try {
            ride.start("1111");                         // wrong OTP
        } catch (IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
        ride.start("4821");
        System.out.println("✔ Correct OTP. Status: " + ride.getStatus());

        Wallet wallet = new Wallet(new BigDecimal("150"));
        try {
            wallet.pay(new BigDecimal("200"));
        } catch (IllegalStateException e) {
            System.out.println("✘ " + e.getMessage());
        }
        System.out.println("Balance unchanged: ₹" + wallet.getBalance());
        wallet.topUp(new BigDecimal("100"));
        wallet.pay(new BigDecimal("200"));
        System.out.println("✔ Paid ₹200. Balance now: ₹" + wallet.getBalance());
    }

    // -----------------------------------------------------------------------
    // 5. ABSTRACTION — abstract class (IS-A + shared data) vs interface (CAN-DO)
    // -----------------------------------------------------------------------
    static void demoAbstraction() {
        header("5. ABSTRACTION");
        // Vehicle v = new Vehicle("Generic", "TN-00");  // compile error: Vehicle is abstract
        System.out.println("✘ new Vehicle(...) -> compile error (abstract class)");
        Vehicle bike = new Bike("Honda Activa", "TN-09-AB-4521");   // upcasting
        System.out.println("✔ " + bike + " | 8 km fare: ₹" + bike.calculateFare(8));

        // Payable p = new Payable();                    // compile error: interface
        Payable upi = new UpiPayment("priya@okaxis");
        System.out.println("✔ Interface in use: " + upi.pay(new BigDecimal("63")));
    }

    // -----------------------------------------------------------------------
    // 6. INHERITANCE
    //    single       : Bike extends Vehicle
    //    multilevel   : Vehicle -> Cab -> CabPremium
    //    hierarchical : User -> RideCustomer, Captain
    //    multiple     : NOT allowed with classes; allowed with interfaces
    // -----------------------------------------------------------------------
    static void demoInheritance() {
        header("6. INHERITANCE");
        CabPremium premium = new CabPremium("Toyota Innova Crysta", "TN-07-BX-1190");
        System.out.println("Multilevel: CabPremium -> "
                + premium.getClass().getSuperclass().getSimpleName() + " -> "
                + premium.getClass().getSuperclass().getSuperclass().getSimpleName());
        System.out.println("CabPremium instanceof Cab? " + (premium instanceof Cab)
                + " | instanceof Vehicle? " + (premium instanceof Vehicle));
        System.out.println("Inherited from Cab -> hasAc(): " + premium.hasAc()
                + " | Overridden seats(): " + premium.seats());

        RideCustomer selvi = new RideCustomer(1, "Selvi", "+91 98401 11111", new BigDecimal("300"));
        Captain murugan = new Captain(2, "Murugan", "+91 98402 22222",
                new Auto("Bajaj RE", "TN-22-CK-7813"), "Egmore");
        System.out.println("Hierarchical: " + selvi.getName() + " -> " + selvi.role()
                + " | " + murugan.getName() + " -> " + murugan.role());

        // class TrackableCaptain extends Captain, GpsDevice {}  // compile error
        TrackableCaptain ravi = new TrackableCaptain(3, "Ravi", "+91 98403 33333",
                new Bike("TVS Jupiter", "TN-10-DE-3344"), "Tambaram");
        System.out.println("Multiple (via interfaces): " + ravi.track() + " | " + ravi.contact());
    }

    // -----------------------------------------------------------------------
    // 7. POLYMORPHISM
    //    compile-time : method overloading (same name, different parameters)
    //    runtime      : method overriding + dynamic dispatch via parent/interface refs
    // -----------------------------------------------------------------------
    static void demoPolymorphism() {
        header("7. POLYMORPHISM");
        System.out.println("a) Runtime - one calculateFare(), different rates:");
        List<Vehicle> fleet = new ArrayList<>();         // parent-type references
        fleet.add(new Bike("Activa", "TN-01"));
        fleet.add(new Auto("RE", "TN-02"));
        fleet.add(new CabEconomy("Dzire", "TN-03"));
        fleet.add(new CabPremium("Innova", "TN-04"));
        for (Vehicle v : fleet) {                        // JVM picks the right method
            System.out.printf("   %-11s 10 km = ₹%s%n", v.getClass().getSimpleName(), v.calculateFare(10));
        }

        System.out.println("b) Runtime - same pay() through the interface:");
        Payable[] methods = {
                new UpiPayment("priya@okaxis"),
                new CashPayment(),
                new WalletPayment(new Wallet(new BigDecimal("500")))
        };
        for (Payable p : methods) {
            System.out.println("   " + p.pay(new BigDecimal("120")));
        }

        System.out.println("c) Compile-time - method overloading:");
        BookingService booking = new BookingService();
        System.out.println("   " + booking.bookRide("Adyar", "Guindy"));
        System.out.println("   " + booking.bookRide("Adyar", "Guindy", "Auto"));
        System.out.println("   " + booking.bookRide("Airport", "Anna Nagar", "CabPremium", "UPI"));

        System.out.println("d) Downcasting safely with instanceof:");
        for (Vehicle v : fleet) {
            if (v instanceof Cab cab) {                  // pattern matching (Java 16+)
                System.out.println("   " + v.getClass().getSimpleName() + " is a Cab, AC = " + cab.hasAc());
            }
        }
    }

    // -----------------------------------------------------------------------
    // 8. COMPOSITION vs AGGREGATION
    //    Composition : the part is created and owned by the whole
    //    Aggregation : the whole only refers to parts that live independently
    // -----------------------------------------------------------------------
    static void demoCompositionAggregation() {
        header("8. COMPOSITION vs AGGREGATION");
        ComposedCaptain senthil = new ComposedCaptain("Senthil", "TVS Jupiter", "TN-11-GH-5566");
        System.out.println("Composition: captain owns -> " + senthil.getVehicle());

        Customer divya = new Customer("Divya", "+91 98404 44444");
        Trip trip = new Trip(divya, senthil);
        System.out.println("Trip created: " + trip);
        trip = null;                                     // the trip ends...
        System.out.println("Aggregation: trip gone, customer still exists -> " + divya.getName());
    }

    // -----------------------------------------------------------------------
    // 9. final — final variable, final method, final class (immutability)
    // -----------------------------------------------------------------------
    static void demoFinal() {
        header("9. final KEYWORD + IMMUTABLE CLASS");
        Location tambaram = Location.of("Tambaram");
        System.out.println("Captain is now at " + tambaram.getName()
                + ". Match radius = " + Location.MATCH_RADIUS_KM + " km");
        for (String place : new String[]{"Egmore", "Chromepet", "Perungalathur"}) {
            Location request = Location.of(place);
            double d = tambaram.distanceTo(request);
            String result = d <= Location.MATCH_RADIUS_KM ? "✔ can be matched" : "✘ outside radius";
            System.out.printf("   Request from %-13s %5.1f km -> %s%n", place, d, result);
        }
        // tambaram.lat = 0;              // compile error: final field
        // class X extends Location {}    // compile error: final class
        System.out.println("Location is final + immutable: fields can't change, class can't be extended");
    }

    // -----------------------------------------------------------------------
    // 10. Object methods — toString(), equals(), hashCode()
    // -----------------------------------------------------------------------
    static void demoObjectMethods() {
        header("10. toString(), equals(), hashCode()");
        Location a = Location.of("Egmore");
        Location b = Location.of("Egmore");
        System.out.println("toString(): " + a);
        System.out.println("a.equals(b)? " + a.equals(b) + " (same value)  |  a == b? "
                + (a == b) + " (different objects)");
        Set<Location> pickups = new HashSet<>();
        pickups.add(a);
        pickups.add(b);
        System.out.println("Unique pickup points in a HashSet: " + pickups.size());
    }

    // -----------------------------------------------------------------------
    // 11. CUSTOM EXCEPTIONS — inheritance applied to errors
    //     checked   (extends Exception)        -> caller MUST handle
    //     unchecked (extends RuntimeException) -> programming/state errors
    // -----------------------------------------------------------------------
    static void demoExceptions() {
        header("11. CUSTOM EXCEPTIONS (checked vs unchecked)");
        MatchingService matching = new MatchingService();
        try {
            matching.findCaptain("Sholinganallur");     // checked: must catch or declare
        } catch (NoCaptainAvailableException e) {
            System.out.println("✘ Checked: " + e.getMessage());
        }
        try {
            new Ride("Egmore", "Egmore", "1234").complete(); // unchecked: state error
        } catch (InvalidRideStatusException e) {
            System.out.println("✘ Unchecked: " + e.getMessage());
        } catch (RideHailingException e) {              // parent catches all app errors
            System.out.println("✘ " + e.getMessage());
        }
    }
}

// ===========================================================================
// Supporting classes
// ===========================================================================

// ---- 1. Class and object --------------------------------------------------
class Customer {
    private final String name;
    private final String phone;

    Customer(String name, String phone) {   // constructor
        this.name = name;                   // 'this' = the current object
        this.phone = phone;
    }

    String greet() { return "Vanakkam " + name + "! Where do you want to go today?"; }
    String getName() { return name; }
    String getPhone() { return phone; }
}

// ---- 2. Constructors ------------------------------------------------------
class RideRequest {
    private final String pickup;
    private final String drop;
    private final String vehicleType;
    private final int passengers;

    RideRequest(String pickup, String drop) {
        this(pickup, drop, "Bike");                          // chains to 3-arg
    }

    RideRequest(String pickup, String drop, String vehicleType) {
        this(pickup, drop, vehicleType, 1);                  // chains to 4-arg
    }

    RideRequest(String pickup, String drop, String vehicleType, int passengers) {
        this.pickup = pickup;                                // the ONE place fields are set
        this.drop = drop;
        this.vehicleType = vehicleType;
        this.passengers = passengers;
    }

    @Override
    public String toString() {
        return String.format("%-10s %s -> %s (%d passenger%s)",
                vehicleType, pickup, drop, passengers, passengers == 1 ? "" : "s");
    }
}

// ---- 3. Static vs instance ------------------------------------------------
class RideCounter {
    static final BigDecimal GST_RATE = new BigDecimal("0.05"); // constant, shared
    private static int totalRides = 0;                          // shared counter

    private final String rideId;                                // per object
    private final String route;

    RideCounter(String pickup, String drop) {
        totalRides++;
        this.rideId = String.format("RIDE-%03d", totalRides);
        this.route = pickup + " -> " + drop;
    }

    static int getTotalRides() { return totalRides; }           // static method
    String getRideId() { return rideId; }
    String getRoute() { return route; }
}

// ---- 4. Encapsulation -----------------------------------------------------
class Ride {
    private final String pickup;
    private final String drop;
    private final String otp;            // private, with NO getter
    private String status = "REQUESTED";
    private int wrongAttempts = 0;

    Ride(String pickup, String drop, String otp) {
        this.pickup = pickup;
        this.drop = drop;
        this.otp = otp;
    }

    String getStatus() { return status; }  // read-only access

    void markArrived() {
        if (!status.equals("REQUESTED")) {
            throw new IllegalStateException("Cannot arrive when ride is " + status);
        }
        status = "CAPTAIN_ARRIVED";
    }

    void start(String enteredOtp) {        // the ONLY way to start a ride
        if (!status.equals("CAPTAIN_ARRIVED")) {
            throw new IllegalStateException("Cannot start a ride that is " + status);
        }
        if (!otp.equals(enteredOtp)) {
            wrongAttempts++;
            if (wrongAttempts == 3) {
                status = "CANCELLED";
                throw new IllegalArgumentException("3 wrong OTPs - ride auto-cancelled");
            }
            throw new IllegalArgumentException("Wrong OTP (attempt " + wrongAttempts + "/3)");
        }
        status = "IN_PROGRESS";
    }

    void complete() {
        if (!status.equals("IN_PROGRESS")) {
            throw new InvalidRideStatusException("Cannot complete " + pickup + " -> " + drop
                    + " ride: it is " + status + ", not IN_PROGRESS");
        }
        status = "COMPLETED";
    }
}

class Wallet {
    private BigDecimal balance;

    Wallet(BigDecimal openingBalance) { this.balance = openingBalance; }

    BigDecimal getBalance() { return balance; }

    void topUp(BigDecimal amount) {        // validation lives INSIDE the class
        if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("Top-up must be between ₹1 and ₹10,000");
        }
        balance = balance.add(amount);
    }

    void pay(BigDecimal amount) {
        if (amount.compareTo(balance) > 0) { // never a partial deduction
            throw new IllegalStateException("Insufficient balance: need ₹" + amount + ", have ₹" + balance);
        }
        balance = balance.subtract(amount);
    }
}

// ---- 5 & 6. Abstraction + inheritance: vehicles ---------------------------
abstract class Vehicle {
    private final String model;
    private final String plate;

    protected Vehicle(String model, String plate) {   // abstract classes CAN have constructors
        this.model = model;
        this.plate = plate;
    }

    abstract BigDecimal baseFare();       // WHAT, not HOW
    abstract BigDecimal perKmRate();
    abstract int seats();

    // concrete method: written ONCE, works for every vehicle (template for polymorphism)
    final BigDecimal calculateFare(double km) {
        return baseFare().add(perKmRate().multiply(BigDecimal.valueOf(km)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + model + " (" + plate + ")";
    }
}

class Bike extends Vehicle {                          // single inheritance
    Bike(String model, String plate) { super(model, plate); }
    @Override BigDecimal baseFare() { return new BigDecimal("15"); }
    @Override BigDecimal perKmRate() { return new BigDecimal("6"); }
    @Override int seats() { return 1; }
}

class Auto extends Vehicle {
    Auto(String model, String plate) { super(model, plate); }
    @Override BigDecimal baseFare() { return new BigDecimal("25"); }
    @Override BigDecimal perKmRate() { return new BigDecimal("11"); }
    @Override int seats() { return 3; }
}

abstract class Cab extends Vehicle {                  // middle level: still abstract
    protected Cab(String model, String plate) { super(model, plate); }
    @Override int seats() { return 4; }
    boolean hasAc() { return true; }                  // cab-only behaviour
}

class CabEconomy extends Cab {
    CabEconomy(String model, String plate) { super(model, plate); }
    @Override BigDecimal baseFare() { return new BigDecimal("50"); }
    @Override BigDecimal perKmRate() { return new BigDecimal("14"); }
}

class CabPremium extends Cab {                        // multilevel: Vehicle -> Cab -> CabPremium
    CabPremium(String model, String plate) { super(model, plate); }
    @Override BigDecimal baseFare() { return new BigDecimal("80"); }
    @Override BigDecimal perKmRate() { return new BigDecimal("20"); }
    @Override int seats() { return 6; }               // overrides Cab's 4
}

// ---- 6. Inheritance: users (hierarchical) ---------------------------------
abstract class User {
    private final int id;
    private final String name;
    private final String phone;

    protected User(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    abstract String role();
    String getName() { return name; }
    int getId() { return id; }
    String getPhone() { return phone; }
}

class RideCustomer extends User {
    private final Wallet wallet;

    RideCustomer(int id, String name, String phone, BigDecimal openingBalance) {
        super(id, name, phone);                       // parent constructor runs first
        this.wallet = new Wallet(openingBalance);
    }

    @Override String role() { return "Customer"; }
}

class Captain extends User {
    private final Vehicle vehicle;
    private String location;

    Captain(int id, String name, String phone, Vehicle vehicle, String location) {
        super(id, name, phone);
        this.vehicle = vehicle;
        this.location = location;
    }

    @Override String role() { return "Captain"; }
    String getLocation() { return location; }
    Vehicle getVehicle() { return vehicle; }
}

// Multiple inheritance of TYPE through interfaces
interface GpsTrackable {
    String track();
}

interface Contactable {
    String contact();
}

class TrackableCaptain extends Captain implements GpsTrackable, Contactable {
    TrackableCaptain(int id, String name, String phone, Vehicle vehicle, String location) {
        super(id, name, phone, vehicle, location);
    }

    @Override public String track() { return getName() + " is at " + getLocation(); }
    @Override public String contact() { return "call " + getPhone(); }
}

// ---- 7. Polymorphism: interface + implementations -------------------------
interface Payable {                                   // a CAN-DO contract, no shared data
    String pay(BigDecimal amount);
}

class UpiPayment implements Payable {
    private final String upiId;
    UpiPayment(String upiId) { this.upiId = upiId; }
    @Override public String pay(BigDecimal amount) { return "₹" + amount + " paid via UPI (" + upiId + ")"; }
}

class CashPayment implements Payable {
    @Override public String pay(BigDecimal amount) { return "₹" + amount + " collected in cash by captain"; }
}

class WalletPayment implements Payable {
    private final Wallet wallet;
    WalletPayment(Wallet wallet) { this.wallet = wallet; }
    @Override public String pay(BigDecimal amount) {
        wallet.pay(amount);
        return "₹" + amount + " paid from wallet (left: ₹" + wallet.getBalance() + ")";
    }
}

class BookingService {                                // method overloading
    String bookRide(String pickup, String drop) {
        return bookRide(pickup, drop, "Bike");
    }

    String bookRide(String pickup, String drop, String vehicle) {
        return bookRide(pickup, drop, vehicle, "choose later");
    }

    String bookRide(String pickup, String drop, String vehicle, String payment) {
        return vehicle + " " + pickup + " -> " + drop + ", payment: " + payment;
    }
}

// ---- 8. Composition vs aggregation ----------------------------------------
class ComposedCaptain {
    private final String name;
    private final Vehicle vehicle;

    ComposedCaptain(String name, String model, String plate) {
        this.name = name;
        this.vehicle = new Bike(model, plate);        // created INSIDE -> composition
    }

    Vehicle getVehicle() { return vehicle; }
    String getName() { return name; }
}

class Trip {
    private final Customer customer;                  // passed IN -> aggregation
    private final ComposedCaptain captain;

    Trip(Customer customer, ComposedCaptain captain) {
        this.customer = customer;
        this.captain = captain;
    }

    @Override
    public String toString() { return customer.getName() + " with captain " + captain.getName(); }
}

// ---- 9 & 10. final class, immutability, equals/hashCode -------------------
final class Location {                                // final class: cannot be extended
    static final double MATCH_RADIUS_KM = 3.0;        // constant

    private final String name;                        // final fields: set once, never change
    private final double lat;
    private final double lng;

    private Location(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    static Location of(String place) {                // static factory method
        switch (place) {
            case "Egmore":        return new Location(place, 13.0732, 80.2609);
            case "Tambaram":      return new Location(place, 12.9249, 80.1000);
            case "Chromepet":     return new Location(place, 12.9516, 80.1462);
            case "Perungalathur": return new Location(place, 12.9046, 80.0961);
            default: throw new IllegalArgumentException("Unknown place: " + place);
        }
    }

    double distanceTo(Location other) {               // Haversine formula
        double r = 6371;
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    String getName() { return name; }

    @Override
    public boolean equals(Object o) {                 // equal by VALUE
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location other = (Location) o;
        return Double.compare(lat, other.lat) == 0 && Double.compare(lng, other.lng) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(lat, lng); }  // must match equals()

    @Override
    public String toString() { return name + " (" + lat + ", " + lng + ")"; }
}

// ---- 11. Custom exceptions -------------------------------------------------
class RideHailingException extends RuntimeException { // base for all app errors
    private static final long serialVersionUID = 1L;
    RideHailingException(String message) { super(message); }
}

class InvalidRideStatusException extends RideHailingException { // unchecked
    private static final long serialVersionUID = 1L;
    InvalidRideStatusException(String message) { super(message); }
}

class NoCaptainAvailableException extends Exception {  // checked: caller must handle
    private static final long serialVersionUID = 1L;
    NoCaptainAvailableException(String message) { super(message); }
}

class MatchingService {
    String findCaptain(String place) throws NoCaptainAvailableException {
        throw new NoCaptainAvailableException("No captain within "
                + Location.MATCH_RADIUS_KM + " km of " + place + " right now");
    }
}