# OOP Concept Map: the teaching script

Each row points to the exact place in the code where a concept appears, with one line on **why** it is there. Paths are relative to `src/com/ridehailing/`. Line numbers are approximate anchors; search for the method name if they drift.

Suggested order for class: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8.

## 1. Classes & objects

| Where | What to show | Why it matters |
|---|---|---|
| `app/ChennaiRideApp.java` `seedPeopleAndVehicles()` | `new Customer(...)`, `new Captain(...)`, `new Auto(...)` | One class, many objects: 5 customers and 12 captains, each with their own state all day. |
| `model/ride/Ride.java` | A `Ride` object per booking | The object carries its own state (status, timeline, route, receipt). Services only ask it to change. |
| `service/*` | Services created once in `ChennaiRideApp` | Objects collaborate by holding references to each other. |

## 2. Constructors: overloading, `this()` chaining, `super()`

| Where | What to show | Why |
|---|---|---|
| `model/vehicle/Vehicle.java:24` | `Vehicle(reg, model)` → `this(reg, model, "White")` | Defaults are written once. The long constructor does all the validation. |
| `model/common/Location.java:31` | `Location(lat, lon)` → `this("GPS point", lat, lon)` | A GPS point without a name reuses the named constructor. |
| `model/user/User.java:26` | `User(id,...)` → `this(id,..., 0, 0)` | A new user has no past ratings. |
| `model/user/Captain.java:42,47` | Three constructors chained with `this(...)` | Simple captain, "short trips only" captain, or veteran with past ratings and dues. |
| `model/user/Captain.java:53`, `Customer.java:39` | `super("CAP-" + nextIdNumber++, ...)` | The User part of the object is built (and validated) first. |
| `model/vehicle/Bike.java:9`, `Cab.java:12`, `CabPremium.java:9` | `super(...)` up the chain | Multilevel: CabPremium → Cab → Vehicle constructors run top-down. |

## 3. Encapsulation

| Where | What to show | Why |
|---|---|---|
| `model/ride/Ride.java` | **No `setStatus()`**; only `assignCaptain`, `markArrived`, `start`, `changeDestination`, `endEarly`, `reachDestination`, `complete`, `markPaymentPending`, `cancel` | The ride guards its own lifecycle. Each method checks the current status (`requireStatus`) and throws `InvalidRideStatusException`. |
| `model/ride/Ride.java:155` `revealOtpTo(Customer)` | The OTP is private; only the ride's own customer can read it | Hiding data plus a validated accessor instead of a getter. |
| `model/user/User.java:55` `protected final moveTo()` | A location changes only inside event methods | `Captain.reachPickup`, `Captain.finishRide`, `Customer.arriveAt` and `Customer.travelOnOwnTo` are the only doors, so a captain can never teleport. |
| `model/user/Captain.java` | `goOnline`, `goOffline`, `acceptRide`, `addDues`, `settleDues` | Intent-revealing methods with rules inside (KYC, busy, ₹500 cash limit). |
| `model/user/Customer.java` | `topUpWallet`, `debitWallet`, `takeOutstandingFees` | Wallet top-ups are limited to ₹0–₹10,000, a debit is all-or-nothing, and fees are handed over exactly once. |
| `model/ride/FareReceipt.java` | Subtotal, GST and total are **derived** in the constructor | The numbers on a receipt can never disagree with each other. |
| `getRideHistory()`, `getOfferLog()`, `getTimeline()` | Return `Collections.unmodifiableList` | Callers can read the list but cannot change it behind the object's back. |

## 4. Abstraction

| Where | What to show | Why |
|---|---|---|
| `model/user/User.java` | `abstract class User` with `abstract getRole()` | "A user" is a concept; only Customer/Captain are real objects. |
| `model/vehicle/Vehicle.java` | Abstract `baseFare()`, `perKmRate()`, `perMinuteRate()`, `minimumFare()`, `seatCapacity()`, `averageSpeedKmph()`, `peakSpeedKmph()` | Vehicle says **what** every vehicle must know; subclasses say **how much**. |
| `model/vehicle/Cab.java` | Abstract class in the middle: adds `abstract luggageCapacity()` | An abstract class can partly implement its parent. |
| `model/payment/Payable.java` | `interface Payable { pay(Money) }` | "Something that can pay" without saying how. |
| `notification/Notifier.java` | `interface Notifier` + a `default handles()` method | "Something that can notify". SMS only overrides `handles()` to filter events. |

## 5. Inheritance

| Where | Type | Why |
|---|---|---|
| `User → Customer`, `User → Captain` | Hierarchical | Shared id/name/phone/location/rating code lives once in User. |
| `Vehicle → Bike`, `Vehicle → Auto`, `Vehicle → Cab` | Hierarchical | Each vehicle reuses `calculateBaseFare()` and `travelMinutes()`. |
| `Vehicle → Cab → CabEconomy / CabPremium` | **Multilevel** | Cab adds shared speeds and AC. `CabPremium` inherits Cab's `peakSpeedKmph()` but overrides `averageSpeedKmph()`. |
| `RideHailingException → InvalidBookingException → ActiveRideExistsException / PaymentPendingException / OutOfServiceAreaException` | Multilevel exceptions | `catch (InvalidBookingException e)` catches every booking rejection (see scene 6 and scene 14 in the demo). |

## 6. Polymorphism

| Kind | Where | Why |
|---|---|---|
| **Overriding: fare rates** | `Bike/Auto/CabEconomy/CabPremium.baseFare()`, `perKmRate()`... | Same method name, different numbers per vehicle. |
| **Central example** | `model/vehicle/Vehicle.java:62` `calculateBaseFare()` (final) | Written once, calls the overridden rates. A bike and an SUV give different fares from the same line. |
| **Overriding `toString`** | `Vehicle.toString`, `Cab.toString` (`super.toString()` + "AC"), `User.toString`, `Captain.toString` (`super.toString()` + vehicle) | Extending the parent's behaviour instead of copying it. |
| **Overloading: `bookRide`** | `service/RideService.java:114,119,124` | Without a payment method (defaults to Cash), with a method, with a coupon. |
| **Overloading: `completeTrip`** | `service/RideService.java:283,288` | Pay with the booked method, or switch at the end of the ride. |
| **Overloading: factories** | `Money.of(long)`, `Money.of(String)`, `Money.of(BigDecimal)`; `Money.times(double)`, `times(BigDecimal)` | Same idea, different input types. |
| **Upcasting** | `app/ChennaiRideApp.java:167` `Vehicle muruganAuto = new Auto(...)`; `FareService.rateCard` `Map<VehicleType, Vehicle>`; `PaymentService.createPayment()` returns `Payable` | Code talks to the general type. |
| **Dynamic dispatch through `Payable`** | `service/PaymentService.java:94` `payable.pay(total)` | UPI, Cash or Wallet code runs, chosen at runtime by the real object. |
| **Dynamic dispatch through `Notifier`** | `service/RideService.java:439` `notify()` loops over `List<Notifier>` | RideService never knows whether it is SMS or push. A WhatsApp notifier would just be a new class. |
| **Overriding with a narrower exception** | `model/payment/WalletPayment.java:20` `pay() throws InsufficientWalletBalanceException`; UPI and Cash declare none | An override may throw fewer checked exceptions than the interface method. |

## 7. Composition vs aggregation

| Where | Relationship | Why |
|---|---|---|
| `model/user/Captain.java:29` `private final Vehicle vehicle` | **Composition**: Captain HAS-A Vehicle | Registered together, never swapped (`final`), meaningless without its captain. A bike captain stays a bike captain. |
| `Ride` → `RideRequest`, `Ride` → `FareReceipt`, `Ride` → `TimelineEntry` list | Composition | Created for this ride only, and they live and die with it. |
| `model/ride/Ride.java` `captain` field and `request.getCustomer()` | **Aggregation**: Ride references a Customer and a Captain | They exist before the ride and carry on after it. |
| `model/user/Customer.java:30` `List<Ride> rideHistory` | Aggregation | The same Ride objects are also held by RideService. |
| `service/RideService` holds `List<Notifier>`; `ChennaiRideApp` owns its services | Aggregation / composition | Objects built from other objects. |

## 8. `static`, `final`, `equals/hashCode/toString`, exceptions, enums

| Concept | Where | Why |
|---|---|---|
| `static` id counters | `Customer.java:25`, `Captain.java:24`, `Ride.java:37` `nextIdNumber` | One counter shared by the class gives unique ids like `CUS-1001`, `CAP-2001`, `RIDE-5001`. |
| `static final` constants | `FareService.GST_PERCENT = 5`, `NIGHT_CHARGE_PERCENT`, `FIRSTRIDE`; `EarningsService.COMMISSION_PERCENT = 20`; `Captain.CASH_DUES_LIMIT`; `RideService.CANCELLATION_GRACE_MINUTES` | Platform rules in one place. |
| `static` initialiser | `model/common/ChennaiPlaces.java` `static { NAMED_AREAS.add(...) }` | Runs once when the class loads. |
| `static` nested class | `EarningsService.DayEarnings`, `PaymentService.UpiOutage` | Helper types that only make sense inside their outer class. |
| `final` class | `Money`, `Location`, `ServiceArea`, `ChennaiPlaces`, `SimulatedClock`, `Bike`, `Auto`, `CabEconomy`, `CabPremium`, `FareReceipt` | Leaf classes and immutable value objects cannot be extended and broken. |
| `final` fields | `Money.amount`, `Captain.vehicle`, `Ride.id`, `Ride.otp` | Set once, never reassigned. |
| `final` methods | `Vehicle.calculateBaseFare`, `User.equals/hashCode`, `User.moveTo` | Subclasses cannot change the formula or the identity rule. |
| **Value objects: equality by value** | `Money.java:102` (₹20.00 == ₹20), `Location.java:80` (same coordinates) | Two equal amounts are interchangeable. |
| **Entities: equality by id** | `User.java:100`, `Ride.java:497`, `Vehicle.java:96` (number plate) | Two objects for the same person or ride are the same thing. |
| `toString` | `FareReceipt.toString()` prints the full boxed receipt; `FareEstimate.toString()` prints a "choose your ride" row | Readable objects make debugging and the demo output easy. |
| **Checked exceptions (deliberate)** | `exception/RideHailingException.java` extends `Exception` | Business outcomes (no captain, wrong OTP, low wallet) must be handled by the caller, so the compiler forces it. |
| **Unchecked for bugs** | `IllegalArgumentException` in `Money`, `Location`, `Vehicle`, `User`; `SimulatedClock.advanceTo` | A negative amount, a bad plate or time going backward is a bug, not a situation to handle. |
| **Enums with fields, constructors and methods** | `VehicleType` (display name, seats, cancellation and no-show fees, `canCarry()`); `RideStatus` (`isActive()`, `canBeCancelled()`); `CaptainStatus` (`canReceiveOffers()`); `CancellationReason` with a nested `Party` enum; `RideEvent` (`isImportant()`) | Enums as small classes, not just named integers. |

## 9. CLI layer (interactive mode)

The same objects power the scripted demo and the live CLI (both build a `ChennaiWorld`). The CLI is written with the same OOP ideas:

| Concept | Where (`cli/`) | Why |
|---|---|---|
| **Abstract class with a concrete loop** | `Menu`: abstract `options()` and `handle(int)`, concrete `final run()` | Every screen shares one loop (status line, options, read, error handling). Subclasses only say what their options are and how to handle one. |
| **Inheritance (hierarchical)** | `StartupMenu`, `MainMenu`, `CustomerMenu`, `CaptainMenu`, `OperationsMenu`, `ClockMenu` all extend `Menu` | Six screens with no copied loop code. |
| **Polymorphism / upcasting** | `MainMenu.handle()`: `Menu next = new CustomerMenu(...)` … `next.run()`; `StartupMenu.startInteractive()`: `Menu main = new MainMenu(...)` | The caller holds a `Menu`; Java runs the right subclass's `options()` and `handle()`. |
| **Overriding optional hooks** | `onEnter()` (pick a customer/captain), `subtitle()`, `exitLabel()` | A subclass overrides a default method only when it needs to. |
| **Encapsulation** | `ConsoleIO` holds the reader, the writer and the echo flag privately; menus use only its methods (`readInt`, `readChoice`, `readYesNo`, `readPlace`, `readMoney`, `readTime`, `printTable`, `printHeader`, `printError`, `printSuccess`) | No menu touches `System.in` directly, so bad input handling lives in one place. |
| **Method overloading** | `ConsoleIO.readInt(prompt, min, max)` and `readInt(prompt, min, max, default)`; `SmsNotifier(clock)` / `SmsNotifier(clock, log)` | Same idea, optional extra parameter. |
| **Composition** | `CliSession` HAS-A `ChennaiWorld` and a `NotificationLog`; `ChennaiWorld` owns every service; the notifiers HAVE-A log | Objects built from objects. |
| **Interface + dynamic dispatch** | The CLI adds no new notifier; `PushNotifier` and `SmsNotifier` write into `NotificationLog` | `RideService` still just loops over `List<Notifier>`. |
| **Unchecked exceptions for navigation** | `GoBackException`, `QuitException` (extend `RuntimeException`) | "b" and "q" are control flow, not business failures. They pass through every prompt helper without `throws` clutter. |
| **Checked business exceptions caught once** | `Menu.run()` catches `RideHailingException` (plus `IllegalArgumentException` / `IllegalStateException` from the model) | One place turns every rule violation into a clean `✘` line. |
| **Enum with fields** | `service/DispatchMode` (`AUTOMATIC` / `CAPTAIN_APP`, each with a label) | The captain mode is a typed value, not a boolean flag. |
| **No business logic in the UI** | Fees: `RideService.cancellationFeeIfCancelledNow`; eligibility: `MatchingService.ineligibilityReason`; clock direction: `SimulatedClock.advanceTo` | The CLI asks the model and prints the answer. It never re-implements a rule. |

## Live exercise: Rapido Parcel (not built on purpose)

Ask students to add parcel delivery **only by extending**:

1. Add a `PARCEL` constant to `VehicleType` (seat capacity 0 or a weight limit).
2. Add a parcel vehicle class (or reuse `Bike`) with its own rates.
3. Model a parcel request: sender and receiver instead of a passenger, and OTP given at delivery.
4. Watch `calculateBaseFare`, `MatchingService` and `PaymentService` work unchanged. That's polymorphism paying off.
