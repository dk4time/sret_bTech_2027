# Simplified class diagram (classroom version)

Type names only: no fields and no methods, grouped for teaching. The full, member-level diagram is `../class_diagram.html`.

## How to open

- Double-click `class_diagram_simplified.html` (or open it from your browser's *File → Open*). It needs an internet connection the first time, because Mermaid is loaded from the jsdelivr CDN.
- The line under the title reads **"9 of 9 diagrams rendered ✔"** when everything has drawn. Each group also shows its own ✔ badge.
- **Projecting:** use the top links to jump between groups. The browser's zoom (Ctrl/Cmd +) makes everything bigger.
- **Printing:** *File → Print* gives one group per A4 landscape page, with its "Why?" box and colours.
- The Mermaid source of each diagram is in `diagrams/` (`01_user_and_vehicles.mmd` … `09_overview.mmd`) and can be pasted into [mermaid.live](https://mermaid.live) or any Mermaid viewer.

## Colours and arrows

| Colour | Kind |
|---|---|
| purple | abstract class («abstract») |
| green | interface («interface») |
| amber | enum («enumeration») |
| light grey | concrete class |
| blue | a whole group (overview only) |

| Arrow | Meaning |
|---|---|
| solid line, hollow triangle | inheritance, "is-a" |
| dashed line, hollow triangle | interface realisation, "can-do" |
| filled diamond ◆ | composition, "has-a, owns" |
| hollow diamond ◇ | aggregation, "has-a, uses" |
| dotted arrow | uses (services → models, and the overview) |

**Rule of thumb:** abstract class = IS-A with shared data · interface = CAN-DO, no shared data.

## Types

| Group | Type | Kind | Why (one line) |
|---|---|---|---|
| User and vehicles | `User` | abstract | Abstract, because there is no plain "user": it holds the shared data (id, name, phone, rating), so it is an abstract class, not an interface. |
| User and vehicles | `Customer` | concrete | Concrete class: a real person who books rides and has their own data (wallet, ride history). |
| User and vehicles | `Captain` | concrete | Concrete class: a real person who drives, with their own data (status, dues, acceptance rate). |
| User and vehicles | `CaptainStatus` | enum | Enum: a fixed set of values (KYC_PENDING, OFFLINE, AVAILABLE, ON_RIDE), so an invalid status is impossible. |
| User and vehicles | `Vehicle` | abstract | Abstract: the fare formula is written once in the parent; each child overrides its own rates (polymorphism). |
| User and vehicles | `Bike` | concrete | Concrete: the cheapest vehicle; it supplies its own rates and speeds. |
| User and vehicles | `Auto` | concrete | Concrete: a three-wheeler with its own rates. |
| User and vehicles | `Cab` | abstract | Abstract, in the middle: it groups Economy and Premium and gives multilevel inheritance. |
| User and vehicles | `CabEconomy` | concrete | Concrete: a sedan cab (level 3 of Vehicle → Cab → CabEconomy). |
| User and vehicles | `CabPremium` | concrete | Concrete: an SUV cab with 6 seats. |
| User and vehicles | `VehicleType` | enum | Enum: a fixed set of values (BIKE, AUTO, CAB_ECONOMY, CAB_PREMIUM), so an invalid value is impossible. |
| Location and money | `Location` | concrete | Immutable: values never change, so it is safe to share one Location between many objects. |
| Location and money | `Money` | concrete | Immutable: values never change, so it is safe to share; it uses BigDecimal to avoid floating-point errors (₹0.1 + ₹0.2 is exactly ₹0.30). |
| Location and money | `ChennaiPlaces` | concrete | Concrete holder of named Chennai places (T. Nagar, Egmore…), created once as constants. |
| Location and money | `ServiceArea` | concrete | Concrete: the Chennai boundary, hotspots and congested zones; it rejects pickups or drops outside the city. |
| Ride | `Ride` | concrete | Guards its own status: there is no setStatus(), only assignCaptain(), start(otp), complete(), cancel(). The OTP is private. This is the best encapsulation example. |
| Ride | `RideStatus` | enum | Enum: a fixed set of values (REQUESTED … COMPLETED, CANCELLED), so an invalid status is impossible. |
| Ride | `FareReceipt` | concrete | The itemised bill. It is created for one ride and belongs to it (composition). |
| Ride | `CancellationReason` | enum | Enum: a fixed list of reasons, each allowed for only one party (customer, captain or system). |
| Payments | `Payable` | interface | Interface: no shared data, only a CAN-DO promise ("can pay"), so a new payment method is one new class and no changes to existing code. |
| Payments | `UpiPayment` | concrete | Can-do Payable: pays by UPI; it can fail when the bank is down. |
| Payments | `CashPayment` | concrete | Can-do Payable: the customer pays the captain in cash; it always succeeds. |
| Payments | `WalletPayment` | concrete | Can-do Payable: pays from the in-app wallet, all or nothing. |
| Payments | `PaymentStatus` | enum | Enum: a fixed set of values (NOT_ATTEMPTED, SUCCESS, FAILED), so an invalid value is impossible. |
| Notifications | `Notifier` | interface | Interface: no shared data, only a CAN-DO promise ("can notify"), so adding WhatsApp or email is one new class. |
| Notifications | `SmsNotifier` | concrete | Can-do Notifier: sends a text message, but only for important events. |
| Notifications | `PushNotifier` | concrete | Can-do Notifier: sends an in-app push for every event. |
| Services | `CustomerService` | concrete | Services coordinate several objects; models enforce their own rules. This one registers customers and handles wallet top-ups. |
| Services | `CaptainService` | concrete | Registers captains, runs KYC, online/offline and dues settlement. |
| Services | `MatchingService` | concrete | Finds the nearest eligible captain and offers the ride. |
| Services | `RideService` | concrete | Runs the ride flow: book → assign → arrive → start → complete → pay. |
| Services | `FareService` | concrete | Calculates surge, estimates and the final receipt. |
| Services | `PaymentService` | concrete | Collects the money through a Payable. |
| Services | `RatingService` | concrete | Ratings for completed rides only, once per side. |
| Services | `EarningsService` | concrete | Splits every payment into captain earnings, commission and GST. |
| Clock and exceptions | `SimulatedClock` | concrete | Controlled time, so demos are repeatable and a night ride is one jump away. |
| Clock and exceptions | `Exception` | concrete | Java's own (JDK) checked exception class: the parent of our base exception. |
| Clock and exceptions | `RideHailingException` | concrete | Inheritance again: catch the parent to handle every app error, or a child to handle one specific error. |
| Clock and exceptions | `InvalidRideStatusException` | concrete | A ride method was called at the wrong moment (e.g. starting a cancelled ride). |
| Clock and exceptions | `InvalidOtpException` | concrete | The captain typed the wrong OTP. |
| Clock and exceptions | `NoCaptainAvailableException` | concrete | Nobody nearby accepted the ride. |
| Clock and exceptions | `CaptainNotEligibleException` | concrete | The captain cannot do this yet (e.g. KYC pending). |
| Clock and exceptions | `CaptainBusyException` | concrete | The captain is on a ride (e.g. cannot go offline). |
| Clock and exceptions | `InsufficientWalletBalanceException` | concrete | The wallet has too little money; nothing is deducted. |
| Clock and exceptions | `InvalidBookingException` | concrete | A booking rule was broken. It is the parent of the three booking errors below (multilevel). |
| Clock and exceptions | `ActiveRideExistsException` | concrete | The customer already has a ride in progress. |
| Clock and exceptions | `PaymentPendingException` | concrete | The customer must first pay for an earlier ride. |
| Clock and exceptions | `OutOfServiceAreaException` | concrete | The pickup or drop is outside Chennai. |
| CLI (interactive mode) | `Menu` | abstract | Abstract: the menu loop is written once; each menu handles its choices differently. |
| CLI (interactive mode) | `StartupMenu` | concrete | Is-a Menu: demo / interactive mode / self-check. |
| CLI (interactive mode) | `MainMenu` | concrete | Is-a Menu: opens the other menus through Menu references (polymorphism). |
| CLI (interactive mode) | `CustomerMenu` | concrete | Is-a Menu: the customer's phone (book, cancel, pay…). |
| CLI (interactive mode) | `CaptainMenu` | concrete | Is-a Menu: the captain's phone (accept, arrive, OTP, complete…). |
| CLI (interactive mode) | `OperationsMenu` | concrete | Is-a Menu: the operations dashboard. |
| CLI (interactive mode) | `ClockMenu` | concrete | Is-a Menu: moves the simulated clock forward. |
| CLI (interactive mode) | `ConsoleIO` | concrete | All keyboard input and screen output, in one place. |
| CLI (interactive mode) | `CliSession` | concrete | The live world (a fresh seeded Chennai) behind the menus. |

## Notes

- Every type name was checked against the code, and all names in the brief match it exactly.
- Added because they are in the code: `StartupMenu`, `InvalidBookingException` (the parent of `ActiveRideExistsException`, `PaymentPendingException` and `OutOfServiceAreaException`), Java's `Exception`, `ConsoleIO` and `CliSession`.
- A `Ride` keeps its `Captain` directly and reaches its `Customer` through its `RideRequest`; both are drawn as aggregation.
- Left out for simplicity: RideRequest, FareEstimate, OutstandingFee, TimelineEntry, PaymentMethod, RideEvent, NotificationLog, DispatchMode, RideAutopilot, ChennaiWorld, ChennaiRideApp, Launcher, GoBackException, QuitException, SelfCheck.
